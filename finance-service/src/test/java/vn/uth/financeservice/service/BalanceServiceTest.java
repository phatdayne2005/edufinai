package vn.uth.financeservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.BalanceResponseDto;
import vn.uth.financeservice.entity.Transaction;
import vn.uth.financeservice.entity.TransactionType;
import vn.uth.financeservice.entity.UserBalance;
import vn.uth.financeservice.repository.TransactionRepository;
import vn.uth.financeservice.repository.UserBalanceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BalanceServiceTest {

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
    }

    @Test
    void testInitializeBalance_Success() {
        // Given
        BigDecimal initialAmount = new BigDecimal("10000000");

        // When
        UserBalance balance = balanceService.initializeBalance(testUserId, initialAmount);

        // Then
        assertNotNull(balance);
        assertEquals(testUserId, balance.getUserId());
        assertEquals(initialAmount, balance.getInitialBalance());
        assertNotNull(balance.getCreatedAt());
    }

    @Test
    void testInitializeBalance_Duplicate_ThrowsException() {
        // Given
        BigDecimal initialAmount = new BigDecimal("10000000");
        balanceService.initializeBalance(testUserId, initialAmount);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            balanceService.initializeBalance(testUserId, new BigDecimal("5000000"));
        });
    }

    @Test
    void testGetCurrentBalance_WithInitialBalance() {
        // Given
        BigDecimal initialBalance = new BigDecimal("10000000");
        balanceService.initializeBalance(testUserId, initialBalance);

        // Create some transactions
        createTransaction(TransactionType.INCOME, new BigDecimal("5000000"), "ACTIVE");
        createTransaction(TransactionType.EXPENSE, new BigDecimal("2000000"), "ACTIVE");
        createTransaction(TransactionType.WITHDRAWAL, new BigDecimal("1000000"), "ACTIVE");

        // When
        BalanceResponseDto response = balanceService.getCurrentBalance(testUserId);

        // Then
        assertNotNull(response);
        assertEquals(initialBalance, response.getInitialBalance());
        assertEquals(new BigDecimal("5000000"), response.getTotalIncome());
        assertEquals(new BigDecimal("2000000"), response.getTotalExpense());
        assertEquals(new BigDecimal("1000000"), response.getTotalWithdrawal());
        // currentBalance = 10000000 + 5000000 - 2000000 - 1000000 = 12000000
        assertEquals(new BigDecimal("12000000"), response.getCurrentBalance());
    }

    @Test
    void testGetCurrentBalance_WithoutInitialBalance() {
        // Given - No initial balance set

        // Create some transactions
        createTransaction(TransactionType.INCOME, new BigDecimal("5000000"), "ACTIVE");
        createTransaction(TransactionType.EXPENSE, new BigDecimal("2000000"), "ACTIVE");

        // When
        BalanceResponseDto response = balanceService.getCurrentBalance(testUserId);

        // Then
        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getInitialBalance());
        assertEquals(new BigDecimal("5000000"), response.getTotalIncome());
        assertEquals(new BigDecimal("2000000"), response.getTotalExpense());
        // currentBalance = 0 + 5000000 - 2000000 = 3000000
        assertEquals(new BigDecimal("3000000"), response.getCurrentBalance());
    }

    @Test
    void testGetCurrentBalance_IgnoresDeletedTransactions() {
        // Given
        BigDecimal initialBalance = new BigDecimal("10000000");
        balanceService.initializeBalance(testUserId, initialBalance);

        // Create active and deleted transactions
        createTransaction(TransactionType.INCOME, new BigDecimal("5000000"), "ACTIVE");
        createTransaction(TransactionType.INCOME, new BigDecimal("3000000"), "DELETED"); // Should be ignored
        createTransaction(TransactionType.EXPENSE, new BigDecimal("2000000"), "ACTIVE");

        // When
        BalanceResponseDto response = balanceService.getCurrentBalance(testUserId);

        // Then
        // Only ACTIVE transactions should be counted
        assertEquals(new BigDecimal("5000000"), response.getTotalIncome());
        assertEquals(new BigDecimal("2000000"), response.getTotalExpense());
        // currentBalance = 10000000 + 5000000 - 2000000 = 13000000
        assertEquals(new BigDecimal("13000000"), response.getCurrentBalance());
    }

    @Test
    void testHasInitializedBalance_True() {
        // Given
        balanceService.initializeBalance(testUserId, new BigDecimal("10000000"));

        // When
        boolean initialized = balanceService.hasInitializedBalance(testUserId);

        // Then
        assertTrue(initialized);
    }

    @Test
    void testHasInitializedBalance_False() {
        // Given - No balance initialized

        // When
        boolean initialized = balanceService.hasInitializedBalance(testUserId);

        // Then
        assertFalse(initialized);
    }

    @Test
    void testGetCurrentBalance_ComplexScenario() {
        // Given
        BigDecimal initialBalance = new BigDecimal("10000000");
        balanceService.initializeBalance(testUserId, initialBalance);

        // Create various transactions
        createTransaction(TransactionType.INCOME, new BigDecimal("5000000"), "ACTIVE");
        createTransaction(TransactionType.INCOME, new BigDecimal("3000000"), "ACTIVE");
        createTransaction(TransactionType.INCOME, new BigDecimal("2000000"), "DELETED"); // Should be ignored
        createTransaction(TransactionType.EXPENSE, new BigDecimal("4000000"), "ACTIVE");
        createTransaction(TransactionType.EXPENSE, new BigDecimal("1000000"), "ACTIVE");
        createTransaction(TransactionType.WITHDRAWAL, new BigDecimal("2000000"), "ACTIVE");
        createTransaction(TransactionType.WITHDRAWAL, new BigDecimal("1000000"), "DELETED"); // Should be ignored

        // When
        BalanceResponseDto response = balanceService.getCurrentBalance(testUserId);

        // Then
        // currentBalance = 10000000 + 5000000 + 3000000 - 4000000 - 1000000 - 2000000 = 11000000
        assertEquals(new BigDecimal("11000000"), response.getCurrentBalance());
        assertEquals(new BigDecimal("8000000"), response.getTotalIncome()); // Only ACTIVE
        assertEquals(new BigDecimal("5000000"), response.getTotalExpense()); // Only ACTIVE
        assertEquals(new BigDecimal("2000000"), response.getTotalWithdrawal()); // Only ACTIVE
    }

    @Test
    void testGetCurrentBalance_WithNegativeResult() {
        // Given
        BigDecimal initialBalance = new BigDecimal("1000000");
        balanceService.initializeBalance(testUserId, initialBalance);

        // Create more expenses than income
        createTransaction(TransactionType.INCOME, new BigDecimal("500000"), "ACTIVE");
        createTransaction(TransactionType.EXPENSE, new BigDecimal("3000000"), "ACTIVE");

        // When
        BalanceResponseDto response = balanceService.getCurrentBalance(testUserId);

        // Then
        // currentBalance = 1000000 + 500000 - 3000000 = -1500000 (negative balance)
        assertEquals(new BigDecimal("-1500000"), response.getCurrentBalance());
    }

    private void createTransaction(TransactionType type, BigDecimal amount, String status) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setUserId(testUserId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setName("Test Transaction");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(status);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }
}

