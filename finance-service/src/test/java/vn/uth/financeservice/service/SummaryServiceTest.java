package vn.uth.financeservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.SummaryResponseDto;
import vn.uth.financeservice.entity.Transaction;
import vn.uth.financeservice.entity.TransactionType;
import vn.uth.financeservice.entity.UserBalance;
import vn.uth.financeservice.repository.TransactionRepository;
import vn.uth.financeservice.repository.UserBalanceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SummaryServiceTest {

    @Autowired
    private SummaryService summaryService;

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
    void testGetMonthlySummary_WithInitialBalance() {
        // Given
        UserBalance balance = new UserBalance();
        balance.setUserId(testUserId);
        balance.setInitialBalance(new BigDecimal("10000000"));
        balance.setCreatedAt(LocalDateTime.now());
        balance.setUpdatedAt(LocalDateTime.now());
        userBalanceRepository.save(balance);

        // Create transactions for current month
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        createTransaction(TransactionType.INCOME, new BigDecimal("15000000"), startOfMonth.plusDays(5), "ACTIVE");
        createTransaction(TransactionType.EXPENSE, new BigDecimal("5000000"), startOfMonth.plusDays(10), "ACTIVE");

        // Create transactions from previous month (should not be counted in monthly)
        createTransaction(TransactionType.INCOME, new BigDecimal("10000000"), startOfMonth.minusMonths(1), "ACTIVE");

        // When
        SummaryResponseDto summary = summaryService.getMonthlySummary(testUserId);

        // Then
        assertNotNull(summary);
        assertEquals(new BigDecimal("15000000"), summary.getMonthlyIncome());
        assertEquals(new BigDecimal("5000000"), summary.getMonthlyExpense());
        // currentBalance = 10000000 (initial) + 15000000 (current month) + 10000000 (prev month) - 5000000 = 30000000
        assertEquals(new BigDecimal("30000000"), summary.getCurrentBalance());
        // savingRate = ((15000000 - 5000000) / 15000000) * 100 = 66.67%
        assertEquals(66.67, summary.getSavingRate(), 0.01);
    }

    @Test
    void testGetMonthlySummary_WithoutInitialBalance() {
        // Given - No initial balance

        // Create transactions for current month
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();

        createTransaction(TransactionType.INCOME, new BigDecimal("10000000"), startOfMonth.plusDays(5), "ACTIVE");
        createTransaction(TransactionType.EXPENSE, new BigDecimal("3000000"), startOfMonth.plusDays(10), "ACTIVE");

        // When
        SummaryResponseDto summary = summaryService.getMonthlySummary(testUserId);

        // Then
        assertNotNull(summary);
        assertEquals(new BigDecimal("10000000"), summary.getMonthlyIncome());
        assertEquals(new BigDecimal("3000000"), summary.getMonthlyExpense());
        // currentBalance = 0 + 10000000 - 3000000 = 7000000
        assertEquals(new BigDecimal("7000000"), summary.getCurrentBalance());
    }

    @Test
    void testGetMonthlySummary_WithWithdrawal() {
        // Given
        UserBalance balance = new UserBalance();
        balance.setUserId(testUserId);
        balance.setInitialBalance(new BigDecimal("5000000"));
        balance.setCreatedAt(LocalDateTime.now());
        balance.setUpdatedAt(LocalDateTime.now());
        userBalanceRepository.save(balance);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();

        createTransaction(TransactionType.INCOME, new BigDecimal("10000000"), startOfMonth.plusDays(5), "ACTIVE");
        createTransaction(TransactionType.EXPENSE, new BigDecimal("2000000"), startOfMonth.plusDays(10), "ACTIVE");
        createTransaction(TransactionType.WITHDRAWAL, new BigDecimal("3000000"), startOfMonth.plusDays(15), "ACTIVE");

        // When
        SummaryResponseDto summary = summaryService.getMonthlySummary(testUserId);

        // Then
        assertNotNull(summary);
        // currentBalance = 5000000 (initial) + 10000000 (income) - 2000000 (expense) - 3000000 (withdrawal) = 10000000
        assertEquals(new BigDecimal("10000000"), summary.getCurrentBalance());
    }

    @Test
    void testGetMonthlySummary_IgnoresDeletedTransactions() {
        // Given
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();

        createTransaction(TransactionType.INCOME, new BigDecimal("10000000"), startOfMonth.plusDays(5), "ACTIVE");
        createTransaction(TransactionType.INCOME, new BigDecimal("5000000"), startOfMonth.plusDays(6), "DELETED"); // Should be ignored
        createTransaction(TransactionType.EXPENSE, new BigDecimal("3000000"), startOfMonth.plusDays(10), "ACTIVE");

        // When
        SummaryResponseDto summary = summaryService.getMonthlySummary(testUserId);

        // Then
        // Only ACTIVE transactions should be counted
        assertEquals(new BigDecimal("10000000"), summary.getMonthlyIncome());
        assertEquals(new BigDecimal("3000000"), summary.getMonthlyExpense());
    }

    @Test
    void testGetMonthlySummary_ZeroMonthlyIncome() {
        // Given - No income in current month
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();

        createTransaction(TransactionType.EXPENSE, new BigDecimal("5000000"), startOfMonth.plusDays(10), "ACTIVE");

        // When
        SummaryResponseDto summary = summaryService.getMonthlySummary(testUserId);

        // Then
        assertNotNull(summary);
        assertEquals(BigDecimal.ZERO, summary.getMonthlyIncome());
        assertEquals(new BigDecimal("5000000"), summary.getMonthlyExpense());
        assertEquals(0.0, summary.getSavingRate()); // Should be 0 when no income
    }

    private void createTransaction(TransactionType type, BigDecimal amount, LocalDateTime date, String status) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID());
        transaction.setUserId(testUserId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setName("Test Transaction");
        transaction.setTransactionDate(date);
        transaction.setStatus(status);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }
}

