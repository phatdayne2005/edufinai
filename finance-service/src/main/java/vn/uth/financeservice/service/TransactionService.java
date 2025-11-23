package vn.uth.financeservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.TransactionRequestDto;
import vn.uth.financeservice.dto.TransactionResponseDto;
import vn.uth.financeservice.entity.*;
import vn.uth.financeservice.repository.CategoryRepository;
import vn.uth.financeservice.repository.GoalRepository;
import vn.uth.financeservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final GoalRepository goalRepository;
    private final GoalService goalService;
    private final BalanceService balanceService;

    @Transactional
    public Transaction createTransaction(UUID userId, TransactionRequestDto request) {
        TransactionType transactionType = TransactionType.valueOf(request.getType());
        
        // Kiểm tra số dư khi tạo EXPENSE transaction
        if (transactionType == TransactionType.EXPENSE) {
            BigDecimal currentBalance = balanceService.getCurrentBalance(userId).getCurrentBalance();
            if (currentBalance.compareTo(request.getAmount()) < 0) {
                throw new RuntimeException("Không đủ số dư. Số dư hiện tại: " + currentBalance);
            }
        }
        
        Transaction t = new Transaction();
        t.setTransactionId(UUID.randomUUID());
        t.setUserId(userId);
        t.setType(transactionType);
        t.setAmount(request.getAmount());
        t.setName(request.getName());
        t.setNote(request.getNote());
        
        // Set category
        Category category;
        // Nếu có goalId và categoryId null → tự động tạo/gán category "Tiết kiệm"
        if (request.getGoalId() != null && request.getCategoryId() == null) {
            category = getOrCreateSavingsCategory(userId);
        } else if (request.getCategoryId() != null) {
            // Nếu có categoryId → dùng category đó
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        } else {
            // Nếu không có goalId và không có categoryId → bắt buộc phải có
            throw new RuntimeException("Category is required when not linking to a goal");
        }
        t.setCategory(category);
        
        // Set transaction date (default to now if not provided)
        t.setTransactionDate(request.getTransactionDate() != null 
                ? request.getTransactionDate() 
                : LocalDateTime.now());
        
        // Gắn transaction vào goal nếu có goalId và là INCOME
        if (request.getGoalId() != null && t.getType() == TransactionType.INCOME) {
            Goal goal = goalRepository.findById(request.getGoalId())
                    .orElseThrow(() -> new RuntimeException("Goal not found"));
            
            // Kiểm tra goal thuộc về user
            if (!goal.getUserId().equals(userId)) {
                throw new RuntimeException("Cannot link transaction to other user's goal");
            }

            // Không cho phép nạp nếu goal đã được xác nhận hoàn thành
            if (goal.getStatus() == GoalStatus.COMPLETED) {
                throw new RuntimeException("Không thể nạp tiền vào mục tiêu đã hoàn thành");
            }

            // Kiểm tra goal đã đủ tiền chưa
            BigDecimal currentSaved = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
            BigDecimal targetAmount = goal.getAmount() != null ? goal.getAmount() : BigDecimal.ZERO;
            BigDecimal remainingAmount = targetAmount.subtract(currentSaved);

            // Nếu goal đã đủ tiền (savedAmount >= amount), không cho phép nạp
            if (currentSaved.compareTo(targetAmount) >= 0) {
                throw new RuntimeException("Mục tiêu đã đủ tiền. Không thể nạp thêm");
            }

            // Tính số tiền thực tế sẽ nạp (nếu nạp > số tiền còn lại, chỉ nạp đủ)
            BigDecimal actualDepositAmount = request.getAmount().min(remainingAmount);
            
            // Kiểm tra số dư khi nạp tiền vào goal
            // Vì nạp vào goal sẽ trừ khỏi số dư (tiền bị khóa), nên cần kiểm tra số dư đủ không
            BigDecimal currentBalance = balanceService.getCurrentBalance(userId).getCurrentBalance();
            if (currentBalance.compareTo(actualDepositAmount) < 0) {
                throw new RuntimeException("Không đủ số dư để nạp vào mục tiêu. Số dư hiện tại: " + currentBalance);
            }
            
            // Cập nhật amount của transaction nếu cần (nếu nạp dư, chỉ nạp đủ)
            if (actualDepositAmount.compareTo(request.getAmount()) < 0) {
                t.setAmount(actualDepositAmount);
            }
            
            t.setGoal(goal);
            
            // Cập nhật saved_amount của goal
            goal.setSavedAmount(currentSaved.add(actualDepositAmount));
            goalRepository.save(goal);
            
            // Tự động check và update status của goal (set newStatus = COMPLETED nếu đạt mục tiêu)
            goalService.checkAndUpdateGoalStatus(goal);
        }
        
        t.setStatus("ACTIVE");
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return transactionRepository.save(t);
    }

    /**
     * Lấy hoặc tạo category "Tiết kiệm" cho user
     * Sử dụng khi nạp tiền vào goal mà không có categoryId
     */
    private Category getOrCreateSavingsCategory(UUID userId) {
        // Tìm category "Tiết kiệm" của user
        return categoryRepository.findByUserIdAndName(userId, "Tiết kiệm")
                .orElseGet(() -> {
                    // Tạo category "Tiết kiệm" nếu chưa có
                    Category savingsCategory = new Category();
                    savingsCategory.setCategoryId(UUID.randomUUID());
                    savingsCategory.setUserId(userId);
                    savingsCategory.setName("Tiết kiệm");
                    savingsCategory.setType(CategoryType.BOTH); // BOTH vì có thể dùng cho cả INCOME và EXPENSE
                    savingsCategory.setIsDefault(false);
                    savingsCategory.setCreatedAt(LocalDateTime.now());
                    return categoryRepository.save(savingsCategory);
                });
    }

    @Transactional
    public void deleteTransaction(UUID transactionId, UUID userId) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!t.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        
        // Kiểm tra nếu transaction có goal và goal đã được xác nhận hoàn thành (COMPLETED)
        // thì không cho phép xóa transaction
        Goal goal = null;
        if (t.getGoal() != null) {
            // Reload goal để đảm bảo có status mới nhất
            goal = goalRepository.findById(t.getGoal().getGoalId())
                    .orElseThrow(() -> new RuntimeException("Goal not found"));
            
            if (goal.getStatus() == GoalStatus.COMPLETED) {
                throw new RuntimeException("Không thể xóa giao dịch của mục tiêu đã hoàn thành");
            }
        }
        
        // Nếu transaction đã được gắn vào goal và là INCOME, trừ lại saved_amount
        if (goal != null && t.getType() == TransactionType.INCOME && "ACTIVE".equals(t.getStatus())) {
            BigDecimal currentSaved = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
            goal.setSavedAmount(currentSaved.subtract(t.getAmount()));
            goalRepository.save(goal);
            
            // Tự động check và update status của goal (có thể chuyển về ACTIVE nếu chưa đạt mục tiêu)
            goalService.checkAndUpdateGoalStatus(goal);
        }
        
        t.setStatus("DELETED");
        t.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(t);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getRecentTransactions(UUID userId, int limit) {
        List<Transaction> transactions = transactionRepository
                .findTopByUserIdAndStatusOrderByTransactionDateDesc(userId, "ACTIVE", limit);

        return transactions.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactions(UUID userId, Pageable pageable, LocalDateTime startDate, LocalDateTime endDate) {
        Page<Transaction> transactions;
        
        if (startDate != null && endDate != null) {
            transactions = transactionRepository.findByUserIdAndStatusAndTransactionDateBetweenOrderByTransactionDateDesc(
                    userId, "ACTIVE", startDate, endDate, pageable);
        } else {
            transactions = transactionRepository.findByUserIdAndStatusOrderByTransactionDateDesc(
                    userId, "ACTIVE", pageable);
        }
        
        return transactions.map(this::toResponseDto);
    }

    private TransactionResponseDto toResponseDto(Transaction t) {
        return new TransactionResponseDto(
                t.getTransactionId(),
                t.getType(),
                t.getName(),
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getNote(),
                t.getAmount(),
                t.getTransactionDate(),
                t.getGoal() != null ? t.getGoal().getGoalId() : null
        );
    }
}
