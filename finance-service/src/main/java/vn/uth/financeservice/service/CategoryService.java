package vn.uth.financeservice.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.CategoryRequestDto;
import vn.uth.financeservice.dto.CategoryTransactionsDto;
import vn.uth.financeservice.dto.TransactionResponseDto;
import vn.uth.financeservice.entity.Category;
import vn.uth.financeservice.entity.CategoryType;
import vn.uth.financeservice.entity.Transaction;
import vn.uth.financeservice.repository.CategoryRepository;
import vn.uth.financeservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Category> getUserCategories(UUID userId) {
        // Lấy cả categories của user và default categories
        return categoryRepository.findByUserIdOrIsDefaultTrue(userId);
    }

    @Transactional
    public Category createCategory(UUID userId, CategoryRequestDto request) {
        // Kiểm tra xem category đã tồn tại chưa
        if (categoryRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new RuntimeException("Category already exists");
        }
        
        Category category = new Category();
        category.setCategoryId(UUID.randomUUID());
        category.setUserId(userId);
        category.setName(request.getName());
        category.setType(request.getType() != null ? CategoryType.valueOf(request.getType()) : CategoryType.EXPENSE);
        category.setIsDefault(false);
        category.setCreatedAt(LocalDateTime.now());
        
        return categoryRepository.save(category);
    }

    /**
     * Xóa category và tự động chuyển tất cả transaction liên quan sang category "Khác"
     * Logic:
     * - Không cho phép xóa category "Khác" (default category, type = BOTH)
     * - Không cho phép xóa default categories khác
     * - Tìm tất cả transaction dùng category đó
     * - Update transaction.category thành category "Khác"
     * - Xóa category
     */
    @Transactional
    public void deleteCategory(UUID categoryId, UUID userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Không cho phép xóa category "Khác" (default category, type = BOTH)
        if ("Khác".equals(category.getName()) && category.getIsDefault()) {
            throw new RuntimeException("Cannot delete default category 'Khác'");
        }
        
        // Không cho phép xóa default categories khác
        if (category.getIsDefault()) {
            throw new RuntimeException("Cannot delete default category");
        }
        
        // Chỉ cho phép xóa category của chính user đó
        if (!category.getUserId().equals(userId)) {
            throw new RuntimeException("Cannot delete other user's category");
        }
        
        // Tìm hoặc tạo category "Khác" (default category, type = BOTH)
        Category otherCategory = getOrCreateOtherCategory();
        
        // Tìm tất cả transaction dùng category này
        List<Transaction> relatedTransactions = transactionRepository.findByCategoryId(categoryId);
        
        // Update tất cả transaction sang category "Khác"
        for (Transaction transaction : relatedTransactions) {
            transaction.setCategory(otherCategory);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
        }
        
        // Flush để đảm bảo tất cả transaction đã được update trước khi xóa category
        entityManager.flush();
        
        // Xóa category
        categoryRepository.delete(category);
    }
    
    /**
     * Tìm hoặc tạo category "Khác" (default category, type = BOTH)
     * Category này luôn tồn tại và không thể xóa
     */
    private Category getOrCreateOtherCategory() {
        return categoryRepository.findByNameAndIsDefaultTrue("Khác")
                .orElseGet(() -> {
                    // Tạo category "Khác" nếu chưa có
                    Category otherCategory = new Category();
                    otherCategory.setCategoryId(UUID.randomUUID());
                    otherCategory.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000000")); // Global user
                    otherCategory.setName("Khác");
                    otherCategory.setType(CategoryType.BOTH);
                    otherCategory.setIsDefault(true);
                    otherCategory.setCreatedAt(LocalDateTime.now());
                    return categoryRepository.save(otherCategory);
                });
    }
    
    /**
     * API: GET /api/v1/categories/{categoryId}/transactions
     * Lấy tất cả transactions của một category trong khoảng thời gian cụ thể
     * 
     * @param categoryId UUID của category
     * @param userId UUID của user (từ JWT)
     * @param month Tháng (1-12), null = tháng hiện tại
     * @param year Năm (2024, 2025...), null = năm hiện tại
     * @param page Số trang (0-based)
     * @param size Số items mỗi trang
     * @return CategoryTransactionsDto với period, summary, và danh sách transactions
     */
    @Transactional(readOnly = true)
    public CategoryTransactionsDto getCategoryTransactions(
            UUID categoryId, UUID userId, Integer month, Integer year,
            int page, int size) {
        
        // 1. Tìm category và kiểm tra quyền truy cập
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Chỉ cho phép xem category của chính user hoặc default categories
        if (!category.getUserId().equals(userId) && !category.getIsDefault()) {
            throw new RuntimeException("Forbidden: Cannot view other user's category");
        }
        
        // 2. Xác định period (tháng hiện tại nếu không truyền month/year)
        YearMonth period = YearMonth.of(
                year != null ? year : LocalDate.now().getYear(),
                month != null ? month : LocalDate.now().getMonthValue()
        );
        LocalDateTime startDate = period.atDay(1).atStartOfDay();
        LocalDateTime endDate = period.atEndOfMonth().atTime(23, 59, 59);
        
        // 3. Lấy tất cả transactions của category trong khoảng thời gian
        List<Transaction> transactions = transactionRepository
                .findByCategoryIdAndStatusAndTransactionDateBetween(
                        categoryId, "ACTIVE", startDate, endDate
                );
        
        // 4. Tính summary (tổng tiền, số lượng, trung bình)
        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageAmount = transactions.isEmpty()
                ? BigDecimal.ZERO
                : totalAmount.divide(
                        BigDecimal.valueOf(transactions.size()),
                        2,
                        RoundingMode.HALF_UP
                );
        
        // 5. Phân trang (in-memory pagination)
        int start = page * size;
        int end = Math.min(start + size, transactions.size());
        List<Transaction> pagedTransactions =
                start < transactions.size()
                        ? transactions.subList(start, end)
                        : List.of();
        
        // 6. Convert sang TransactionResponseDto
        List<TransactionResponseDto> transactionDtos = pagedTransactions.stream()
                .map(this::toTransactionResponseDto)
                .collect(Collectors.toList());
        
        // 7. Tạo response
        CategoryTransactionsDto.PeriodInfo periodInfo =
                new CategoryTransactionsDto.PeriodInfo(
                        period.getMonthValue(),
                        period.getYear(),
                        period.atDay(1),
                        period.atEndOfMonth()
                );
        
        CategoryTransactionsDto.TransactionSummary summary =
                new CategoryTransactionsDto.TransactionSummary(
                        totalAmount,
                        transactions.size(),
                        averageAmount
                );
        
        return new CategoryTransactionsDto(
                category.getCategoryId(),
                category.getName(),
                category.getType(),
                periodInfo,
                summary,
                transactionDtos
        );
    }
    
    /**
     * Helper method: Convert Transaction entity sang TransactionResponseDto
     */
    private TransactionResponseDto toTransactionResponseDto(Transaction transaction) {
        return new TransactionResponseDto(
                transaction.getTransactionId(),
                transaction.getType(),
                transaction.getName(),
                transaction.getCategory() != null ? transaction.getCategory().getName() : null,
                transaction.getNote(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getGoal() != null ? transaction.getGoal().getGoalId() : null
        );
    }
}

