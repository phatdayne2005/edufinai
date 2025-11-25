package vn.uth.financeservice.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.GoalRequestDto;
import vn.uth.financeservice.dto.GoalStatusUpDate;
import vn.uth.financeservice.dto.GoalWithdrawRequestDto;
import vn.uth.financeservice.dto.GoalTransactionHistoryDto;
import vn.uth.financeservice.dto.TransactionResponseDto;
import vn.uth.financeservice.entity.Goal;
import vn.uth.financeservice.entity.GoalStatus;
import vn.uth.financeservice.entity.Transaction;
import vn.uth.financeservice.entity.TransactionType;
import vn.uth.financeservice.repository.GoalRepository;
import vn.uth.financeservice.repository.TransactionRepository;
import vn.uth.financeservice.repository.CategoryRepository;
import vn.uth.financeservice.entity.Category;
import vn.uth.financeservice.entity.CategoryType;
import vn.uth.financeservice.client.GamificationServiceClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;
    private final GamificationServiceClient gamificationServiceClient;

    @Transactional
    public Goal createGoal(UUID userId, GoalRequestDto request) {
        Goal g = new Goal();
        g.setGoalId(UUID.randomUUID());
        g.setUserId(userId);
        g.setTitle(request.getTitle());
        g.setAmount(request.getAmount());
        g.setStartAt(request.getStartAt() != null ? request.getStartAt() : LocalDateTime.now());
        g.setEndAt(request.getEndAt());
        g.setStatus(GoalStatus.ACTIVE);
        g.setNewStatus(GoalStatus.ACTIVE); // Set newStatus để tránh lỗi NOT NULL
        g.setUpdatedAt(LocalDateTime.now()); // tránh lỗi NOT NULL
        return goalRepository.save(g);
    }

    /**
     * Xác nhận hoàn thành mục tiêu
     * Chỉ cho phép xác nhận khi savedAmount >= amount
     * Sau khi xác nhận, goal chuyển sang COMPLETED và không thể thao tác nữa
     */
    @Transactional
    public Goal confirmCompletion(UUID goalId, UUID userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        if (!goal.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }

        // Kiểm tra goal đã đủ tiền chưa
        BigDecimal savedAmount = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
        BigDecimal targetAmount = goal.getAmount() != null ? goal.getAmount() : BigDecimal.ZERO;
        
        if (savedAmount.compareTo(targetAmount) < 0) {
            throw new RuntimeException("Mục tiêu chưa đủ tiền. Số tiền hiện có: " + savedAmount + ", cần: " + targetAmount);
        }

        // Chỉ cho phép xác nhận nếu goal chưa COMPLETED
        if (goal.getStatus() == GoalStatus.COMPLETED) {
            throw new RuntimeException("Mục tiêu đã được xác nhận hoàn thành");
        }

        // Chuyển goal sang COMPLETED
        goal.setStatus(GoalStatus.COMPLETED);
        goal.setNewStatus(GoalStatus.COMPLETED);
        goal.setUpdatedAt(LocalDateTime.now());

        Goal savedGoal = goalRepository.save(goal);

        // Publish event đến gamification service để ghi nhận challenge progress
        gamificationServiceClient.publishGoalAchievedEvent(userId, "GOAL", "COMPLETE");

        return savedGoal;
    }

    /**
     * Kiểm tra và cập nhật status của goal dựa trên savedAmount và endAt
     * Logic:
     * - Nếu savedAmount >= amount → newStatus = COMPLETED (nhưng không tự động chuyển, cần user xác nhận)
     * - Nếu endAt < now và savedAmount < amount → FAILED
     * - Còn lại → ACTIVE
     * 
     * Note: Method này chỉ check và set newStatus, không tự động chuyển sang COMPLETED
     * User phải xác nhận hoàn thành thông qua API confirm-completion
     */
    @Transactional
    public Goal checkAndUpdateGoalStatus(Goal goal) {
        if (goal == null) {
            return null;
        }

        // Không check và update nếu goal đã FAILED hoặc đã COMPLETED (đã xác nhận)
        if (goal.getStatus() == GoalStatus.FAILED || goal.getStatus() == GoalStatus.COMPLETED) {
            return goal;
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal savedAmount = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
        BigDecimal targetAmount = goal.getAmount() != null ? goal.getAmount() : BigDecimal.ZERO;
        LocalDateTime endAt = goal.getEndAt();

        GoalStatus newStatus = goal.getStatus();
        GoalStatus newStatusFlag = goal.getNewStatus();

        // Check: Nếu đã đạt mục tiêu (savedAmount >= amount) → set newStatus = COMPLETED (nhưng không chuyển status)
        if (savedAmount.compareTo(targetAmount) >= 0 && targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            newStatusFlag = GoalStatus.COMPLETED; // Chỉ set flag, không chuyển status
            // Status vẫn giữ ACTIVE để user có thể xác nhận
        }
        // Check: Nếu hết hạn mà chưa đạt mục tiêu → FAILED
        else if (endAt != null && endAt.isBefore(now) && savedAmount.compareTo(targetAmount) < 0) {
            newStatus = GoalStatus.FAILED;
            newStatusFlag = GoalStatus.FAILED;
        }
        // Còn lại → ACTIVE (nếu savedAmount < amount và chưa hết hạn)
        else {
            newStatus = GoalStatus.ACTIVE;
            newStatusFlag = GoalStatus.ACTIVE;
        }

        // Update nếu có thay đổi
        boolean needUpdate = false;
        if (newStatus != goal.getStatus()) {
            goal.setStatus(newStatus);
            needUpdate = true;
        }
        if (newStatusFlag != goal.getNewStatus()) {
            goal.setNewStatus(newStatusFlag);
            needUpdate = true;
        }
        
        if (needUpdate) {
            goal.setUpdatedAt(now);
            return goalRepository.save(goal);
        }

        return goal;
    }

    @Transactional(readOnly = true)
    public List<Goal> getUserGoals(UUID userId) {
        // Method này chỉ đọc, không update status
        // Sử dụng getUserGoalsWithAutoStatusUpdate() nếu muốn tự động update status
        return goalRepository.findByUserId(userId);
    }

    /**
     * Lấy danh sách goals và tự động check/update status
     * Sử dụng method này thay vì getUserGoals() nếu muốn tự động update status
     */
    @Transactional
    public List<Goal> getUserGoalsWithAutoStatusUpdate(UUID userId) {
        List<Goal> goals = goalRepository.findByUserId(userId);
        
        // Check và update status cho từng goal
        return goals.stream()
                .map(this::checkAndUpdateGoalStatus)
                .collect(Collectors.toList());
    }

    /**
     * Rút tiền từ goal
     * Tạo WITHDRAWAL transaction và giảm savedAmount của goal
     */
    @Transactional
    public Transaction withdrawFromGoal(UUID goalId, GoalWithdrawRequestDto request, UUID userId) {
        // Tìm goal
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Kiểm tra quyền sở hữu
        if (!goal.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }

        // Không cho phép rút nếu goal đã được xác nhận hoàn thành
        if (goal.getStatus() == GoalStatus.COMPLETED) {
            throw new RuntimeException("Không thể rút tiền từ mục tiêu đã hoàn thành");
        }

        // Kiểm tra số tiền có thể rút
        BigDecimal savedAmount = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
        BigDecimal withdrawAmount = request.getAmount();

        if (savedAmount.compareTo(withdrawAmount) < 0) {
            throw new RuntimeException(
                    String.format("Không đủ số tiền trong mục tiêu. Số tiền có thể rút: %s", savedAmount)
            );
        }

        // Tìm category "Rút tiền" hoặc tạo mới nếu chưa có
        Category withdrawalCategory = categoryRepository
                .findByUserIdAndName(userId, "Rút tiền")
                .orElse(null);

        if (withdrawalCategory == null) {
            // Tạo category "Rút tiền" nếu chưa có
            withdrawalCategory = new Category();
            withdrawalCategory.setCategoryId(UUID.randomUUID());
            withdrawalCategory.setUserId(userId);
            withdrawalCategory.setName("Rút tiền");
            withdrawalCategory.setType(CategoryType.EXPENSE); // EXPENSE vì rút tiền là một dạng chi tiêu
            withdrawalCategory.setIsDefault(false);
            withdrawalCategory.setCreatedAt(LocalDateTime.now());
            withdrawalCategory = categoryRepository.save(withdrawalCategory);
        }

        // Tạo WITHDRAWAL transaction
        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setUserId(userId);
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setAmount(withdrawAmount);
        transaction.setName("Rút tiền từ mục tiêu \"" + goal.getTitle() + "\"");
        transaction.setNote(request.getNote());
        transaction.setCategory(withdrawalCategory);
        transaction.setGoal(goal);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus("ACTIVE");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        // Giảm savedAmount của goal
        goal.setSavedAmount(savedAmount.subtract(withdrawAmount));
        goal.setUpdatedAt(LocalDateTime.now());
        goalRepository.save(goal);

        // Tự động check và update goal status
        checkAndUpdateGoalStatus(goal);

        // Lưu transaction
        return transactionRepository.save(transaction);
    }

    /**
     * Lấy lịch sử giao dịch của goal
     * Bao gồm thông tin goal và danh sách tất cả transactions (nạp + rút)
     */
    @Transactional(readOnly = true)
    public GoalTransactionHistoryDto getGoalTransactionHistory(UUID goalId, UUID userId) {
        // Tìm goal
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Kiểm tra quyền sở hữu
        if (!goal.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }

        // Lấy tất cả transactions của goal (chỉ ACTIVE)
        List<Transaction> transactions = transactionRepository.findByGoalId(goalId)
                .stream()
                .filter(t -> "ACTIVE".equals(t.getStatus()))
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .collect(Collectors.toList());

        // Convert sang DTO
        List<TransactionResponseDto> transactionDtos = transactions.stream()
                .map(this::toTransactionResponseDto)
                .collect(Collectors.toList());

        // Tính tổng nạp và rút
        BigDecimal totalDeposit = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawal = transactions.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tạo summary
        GoalTransactionHistoryDto.TransactionSummary summary = 
            new GoalTransactionHistoryDto.TransactionSummary(
                totalDeposit, 
                totalWithdrawal, 
                transactions.size()
            );

        // Tạo response
        return new GoalTransactionHistoryDto(
            goal.getTitle(),
            goal.getAmount(),
            goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO,
            transactionDtos,
            summary
        );
    }

    /**
     * Convert Transaction entity sang TransactionResponseDto
     */
    private TransactionResponseDto toTransactionResponseDto(Transaction t) {
        TransactionResponseDto dto = new TransactionResponseDto(
                t.getTransactionId(),
                t.getType(),
                t.getName(),
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getNote(),
                t.getAmount(),
                t.getTransactionDate(),
                t.getGoal() != null ? t.getGoal().getGoalId() : null
        );
        return dto;
    }

    /**
     * Xóa goal và tất cả transaction liên quan đến goal
     * Logic:
     * - Tìm tất cả transaction liên quan đến goal
     * - Xóa tất cả transaction đó
     * - Xóa goal
     * 
     * Số dư sẽ tự động đúng vì:
     * - Xóa transaction INCOME có goalId → totalGoalDeposit giảm → số dư tăng (tiền không còn bị khóa)
     * - Xóa transaction WITHDRAWAL có goalId → totalWithdrawal giảm → số dư giảm (không còn cộng nữa)
     * - Kết quả: Số dư vẫn đúng, không cần tạo WITHDRAWAL transaction mới
     */
    @Transactional
    public void deleteGoal(UUID goalId, UUID userId) {
        // Tìm goal
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        // Kiểm tra quyền sở hữu
        if (!goal.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }

        // Không cho phép xóa nếu goal đã được xác nhận hoàn thành
        if (goal.getStatus() == GoalStatus.COMPLETED) {
            throw new RuntimeException("Không thể xóa mục tiêu đã hoàn thành");
        }

        // Tìm tất cả transaction liên quan đến goal
        List<Transaction> relatedTransactions = transactionRepository.findByGoalId(goalId);

        // Xóa tất cả transaction liên quan đến goal
        for (Transaction transaction : relatedTransactions) {
            transactionRepository.delete(transaction);
        }

        // Flush để đảm bảo tất cả transaction đã được xóa trước khi xóa goal
        // Điều này fix lỗi Hibernate TransientObjectException và foreign key constraint
        entityManager.flush();

        // Xóa goal khỏi database
        goalRepository.delete(goal);
    }
}
