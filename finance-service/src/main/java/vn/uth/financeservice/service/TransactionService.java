package vn.uth.financeservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.TransactionRequestDto;
import vn.uth.financeservice.dto.TransactionResponseDto;
import vn.uth.financeservice.entity.Category;
import vn.uth.financeservice.entity.Goal;
import vn.uth.financeservice.entity.Transaction;
import vn.uth.financeservice.entity.TransactionType;
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

    @Transactional
    public Transaction createTransaction(UUID userId, TransactionRequestDto request) {
        Transaction t = new Transaction();
        t.setTransactionId(UUID.randomUUID());
        t.setUserId(userId);
        t.setType(TransactionType.valueOf(request.getType()));
        t.setAmount(request.getAmount());
        t.setName(request.getName());
        t.setNote(request.getNote());
        
        // Set category (optional)
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            t.setCategory(category);
        } else {
            t.setCategory(null);
        }
        
        // Set transaction date (default to now if not provided)
        t.setTransactionDate(request.getTransactionDate() != null 
                ? request.getTransactionDate() 
                : LocalDateTime.now());
        
        // Gắn transaction vào goal nếu có goalId và là INCOME
        if (request.getGoalId() != null && t.getType() == TransactionType.INCOME) {
            Goal goal = goalRepository.findById(request.getGoalId())
                    .orElseThrow(() -> new RuntimeException("Goal not found"));
            t.setGoal(goal);
            
            // Cập nhật saved_amount của goal
            BigDecimal currentSaved = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
            goal.setSavedAmount(currentSaved.add(request.getAmount()));
            goalRepository.save(goal);
            
            // Tự động check và update status của goal (COMPLETED nếu đạt mục tiêu)
            goalService.checkAndUpdateGoalStatus(goal);
        }
        
        t.setStatus("ACTIVE");
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return transactionRepository.save(t);
    }

    @Transactional
    public void deleteTransaction(UUID transactionId, UUID userId) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (!t.getUserId().equals(userId)) {
            throw new RuntimeException("Forbidden");
        }
        
        // Nếu transaction đã được gắn vào goal và là INCOME, trừ lại saved_amount
        if (t.getGoal() != null && t.getType() == TransactionType.INCOME && "ACTIVE".equals(t.getStatus())) {
            Goal goal = t.getGoal();
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
