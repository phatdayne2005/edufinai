package vn.uth.financeservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.TransactionRequestDto;
import vn.uth.financeservice.entity.*;
import vn.uth.financeservice.repository.CategoryRepository;
import vn.uth.financeservice.repository.GoalRepository;
import vn.uth.financeservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private GoalRepository goalRepository;

    private UUID testUserId;
    private Category testCategory;
    private Goal testGoal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Create test category
        testCategory = new Category();
        testCategory.setCategoryId(UUID.randomUUID());
        testCategory.setUserId(testUserId);
        testCategory.setName("Test Category");
        testCategory.setIsDefault(false);
        testCategory.setCreatedAt(LocalDateTime.now());
        testCategory = categoryRepository.save(testCategory);

        // Create test goal
        testGoal = new Goal();
        testGoal.setGoalId(UUID.randomUUID());
        testGoal.setUserId(testUserId);
        testGoal.setTitle("Test Goal");
        testGoal.setAmount(new BigDecimal("20000000"));
        testGoal.setSavedAmount(BigDecimal.ZERO);
        testGoal.setStartAt(LocalDateTime.now());
        testGoal.setEndAt(LocalDateTime.now().plusMonths(6));
        testGoal.setStatus(GoalStatus.ACTIVE);
        testGoal.setNewStatus(GoalStatus.ACTIVE);
        testGoal.setUpdatedAt(LocalDateTime.now());
        testGoal = goalRepository.save(testGoal);
    }

    @Test
    void testCreateTransaction_INCOME_Success() {
        // Given
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("INCOME");
        request.setAmount(new BigDecimal("5000000"));
        request.setName("Lương tháng 1");
        request.setCategoryId(testCategory.getCategoryId());
        request.setNote("Lương cơ bản");

        // When
        Transaction transaction = transactionService.createTransaction(testUserId, request);

        // Then
        assertNotNull(transaction);
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertEquals(new BigDecimal("5000000"), transaction.getAmount());
        assertEquals("Lương tháng 1", transaction.getName());
        assertEquals("ACTIVE", transaction.getStatus());
        assertNotNull(transaction.getTransactionDate());
    }

    @Test
    void testCreateTransaction_INCOME_WithGoal() {
        // Given
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("INCOME");
        request.setAmount(new BigDecimal("5000000"));
        request.setName("Tiết kiệm");
        request.setCategoryId(testCategory.getCategoryId());
        request.setGoalId(testGoal.getGoalId());

        // When
        Transaction transaction = transactionService.createTransaction(testUserId, request);

        // Then
        assertNotNull(transaction);
        assertEquals(testGoal.getGoalId(), transaction.getGoal().getGoalId());
        
        // Verify goal savedAmount increased
        Goal updatedGoal = goalRepository.findById(testGoal.getGoalId()).orElseThrow();
        assertEquals(new BigDecimal("5000000"), updatedGoal.getSavedAmount());
    }

    @Test
    void testCreateTransaction_EXPENSE_Success() {
        // Given
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("EXPENSE");
        request.setAmount(new BigDecimal("2000000"));
        request.setName("Mua sắm");
        request.setCategoryId(testCategory.getCategoryId());

        // When
        Transaction transaction = transactionService.createTransaction(testUserId, request);

        // Then
        assertNotNull(transaction);
        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertEquals(new BigDecimal("2000000"), transaction.getAmount());
        assertNull(transaction.getGoal()); // EXPENSE should not have goal
    }

    @Test
    void testDeleteTransaction_WithGoal() {
        // Given
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("INCOME");
        request.setAmount(new BigDecimal("5000000"));
        request.setName("Tiết kiệm");
        request.setCategoryId(testCategory.getCategoryId());
        request.setGoalId(testGoal.getGoalId());
        Transaction transaction = transactionService.createTransaction(testUserId, request);
        
        // Verify goal savedAmount increased
        Goal goalBefore = goalRepository.findById(testGoal.getGoalId()).orElseThrow();
        assertEquals(new BigDecimal("5000000"), goalBefore.getSavedAmount());

        // When
        transactionService.deleteTransaction(transaction.getTransactionId(), testUserId);

        // Then
        Transaction deletedTransaction = transactionRepository.findById(transaction.getTransactionId()).orElseThrow();
        assertEquals("DELETED", deletedTransaction.getStatus());
        
        // Verify goal savedAmount decreased
        Goal goalAfter = goalRepository.findById(testGoal.getGoalId()).orElseThrow();
        assertEquals(BigDecimal.ZERO, goalAfter.getSavedAmount());
    }

    @Test
    void testDeleteTransaction_UnauthorizedUser_ThrowsException() {
        // Given
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("INCOME");
        request.setAmount(new BigDecimal("5000000"));
        request.setName("Test");
        request.setCategoryId(testCategory.getCategoryId());
        Transaction transaction = transactionService.createTransaction(testUserId, request);
        
        UUID otherUserId = UUID.randomUUID();

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            transactionService.deleteTransaction(transaction.getTransactionId(), otherUserId);
        });
    }

    @Test
    void testCreateTransaction_CategoryNotFound_ThrowsException() {
        // Given
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("INCOME");
        request.setAmount(new BigDecimal("5000000"));
        request.setName("Test");
        request.setCategoryId(UUID.randomUUID()); // Non-existent category

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            transactionService.createTransaction(testUserId, request);
        });
    }

    @Test
    void testCreateTransaction_GoalNotFound_ThrowsException() {
        // Given
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("INCOME");
        request.setAmount(new BigDecimal("5000000"));
        request.setName("Test");
        request.setCategoryId(testCategory.getCategoryId());
        request.setGoalId(UUID.randomUUID()); // Non-existent goal

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            transactionService.createTransaction(testUserId, request);
        });
    }

    @Test
    void testCreateTransaction_EXPENSE_WithGoalId_IgnoresGoal() {
        // Given
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("EXPENSE");
        request.setAmount(new BigDecimal("2000000"));
        request.setName("Mua sắm");
        request.setCategoryId(testCategory.getCategoryId());
        request.setGoalId(testGoal.getGoalId()); // Should be ignored for EXPENSE

        // When
        Transaction transaction = transactionService.createTransaction(testUserId, request);

        // Then
        assertNotNull(transaction);
        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertNull(transaction.getGoal()); // EXPENSE should not have goal
        
        // Verify goal savedAmount unchanged
        Goal goal = goalRepository.findById(testGoal.getGoalId()).orElseThrow();
        assertEquals(BigDecimal.ZERO, goal.getSavedAmount());
    }

    @Test
    void testCreateTransaction_INCOME_WithOtherUserGoal_ThrowsException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        Goal otherUserGoal = new Goal();
        otherUserGoal.setGoalId(UUID.randomUUID());
        otherUserGoal.setUserId(otherUserId);
        otherUserGoal.setTitle("Other User Goal");
        otherUserGoal.setAmount(new BigDecimal("10000000"));
        otherUserGoal.setSavedAmount(BigDecimal.ZERO);
        otherUserGoal.setStartAt(LocalDateTime.now());
        otherUserGoal.setEndAt(LocalDateTime.now().plusMonths(6));
        otherUserGoal.setStatus(GoalStatus.ACTIVE);
        otherUserGoal.setNewStatus(GoalStatus.ACTIVE);
        otherUserGoal.setUpdatedAt(LocalDateTime.now());
        otherUserGoal = goalRepository.save(otherUserGoal);

        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("INCOME");
        request.setAmount(new BigDecimal("5000000"));
        request.setName("Test");
        request.setCategoryId(testCategory.getCategoryId());
        request.setGoalId(otherUserGoal.getGoalId());

        // When & Then - Should throw exception because goal belongs to different user
        assertThrows(RuntimeException.class, () -> {
            transactionService.createTransaction(testUserId, request);
        });
        
        // Verify goal savedAmount unchanged
        Goal goal = goalRepository.findById(otherUserGoal.getGoalId()).orElseThrow();
        assertEquals(BigDecimal.ZERO, goal.getSavedAmount());
    }

    @Test
    void testDeleteTransaction_NotFound_ThrowsException() {
        // Given
        UUID nonExistentTransactionId = UUID.randomUUID();

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            transactionService.deleteTransaction(nonExistentTransactionId, testUserId);
        });
    }

    @Test
    void testGetRecentTransactions_ReturnsLimitedResults() {
        // Given - Create multiple transactions
        for (int i = 0; i < 10; i++) {
            final int dayOffset = i; // Make effectively final for lambda
            TransactionRequestDto request = new TransactionRequestDto();
            request.setType("INCOME");
            request.setAmount(new BigDecimal("1000000"));
            request.setName("Transaction " + dayOffset);
            request.setCategoryId(testCategory.getCategoryId());
            request.setTransactionDate(LocalDateTime.now().minusDays(dayOffset));
            transactionService.createTransaction(testUserId, request);
        }

        // When
        var recentTransactions = transactionService.getRecentTransactions(testUserId, 5);

        // Then
        assertEquals(5, recentTransactions.size());
        // Should be ordered by date descending (newest first)
    }

    @Test
    void testGetTransactions_WithDateRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(10);
        LocalDateTime endDate = LocalDateTime.now().minusDays(5);

        // Create transactions inside and outside date range
        TransactionRequestDto insideRequest = new TransactionRequestDto();
        insideRequest.setType("INCOME");
        insideRequest.setAmount(new BigDecimal("1000000"));
        insideRequest.setName("Inside Range");
        insideRequest.setCategoryId(testCategory.getCategoryId());
        insideRequest.setTransactionDate(LocalDateTime.now().minusDays(7));
        transactionService.createTransaction(testUserId, insideRequest);

        TransactionRequestDto outsideRequest = new TransactionRequestDto();
        outsideRequest.setType("INCOME");
        outsideRequest.setAmount(new BigDecimal("1000000"));
        outsideRequest.setName("Outside Range");
        outsideRequest.setCategoryId(testCategory.getCategoryId());
        outsideRequest.setTransactionDate(LocalDateTime.now().minusDays(15));
        transactionService.createTransaction(testUserId, outsideRequest);

        // When
        var transactions = transactionService.getTransactions(
                testUserId,
                org.springframework.data.domain.PageRequest.of(0, 10),
                startDate,
                endDate
        );

        // Then
        assertEquals(1, transactions.getTotalElements());
        assertEquals("Inside Range", transactions.getContent().get(0).getName());
    }
}

