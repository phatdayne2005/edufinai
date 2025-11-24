package vn.uth.financeservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.*;
import vn.uth.financeservice.entity.*;
import vn.uth.financeservice.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TransactionRepository transactionRepository;
    private final GoalRepository goalRepository;
    private final BalanceService balanceService;

    @Transactional(readOnly = true)
    public SummaryResponseDto getMonthlySummary(UUID userId) {
        // Lấy tháng hiện tại
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Tính tổng thu nhập trong tháng (chỉ ACTIVE)
        BigDecimal monthlyIncome = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.INCOME, "ACTIVE", startOfMonth, endOfMonth)
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính tổng chi tiêu trong tháng (chỉ ACTIVE)
        BigDecimal monthlyExpense = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, "ACTIVE", startOfMonth, endOfMonth)
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Số dư hiện tại = tổng thu nhập - tổng chi tiêu (tất cả thời gian, chỉ ACTIVE)
        BigDecimal totalIncome = transactionRepository
                .findByUserIdAndTypeAndStatus(userId, TransactionType.INCOME, "ACTIVE")
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactionRepository
                .findByUserIdAndTypeAndStatus(userId, TransactionType.EXPENSE, "ACTIVE")
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentBalance = totalIncome.subtract(totalExpense);

        // Tỷ lệ tiết kiệm = (số dư / thu nhập) * 100
        double savingRate = 0.0;
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balance = monthlyIncome.subtract(monthlyExpense);
            savingRate = balance.divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return new SummaryResponseDto(currentBalance, monthlyIncome, monthlyExpense, savingRate);
    }

    @Transactional(readOnly = true)
    public MonthOptimizedResponseDto getMonthOptimizedSummary(UUID userId) {
        // Lấy tháng hiện tại
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        LocalDateTime startOfMonth = startDate.atStartOfDay();
        LocalDateTime endOfMonth = endDate.atTime(23, 59, 59);

        // Lấy tháng trước để tính trends
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDateTime startOfPreviousMonth = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfPreviousMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59);

        // 1. Tính period
        PeriodDto period = new PeriodDto(startDate, endDate);

        // 2. Tính summary
        // Lấy tất cả transactions trong tháng (ACTIVE)
        List<Transaction> monthTransactions = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.INCOME, "ACTIVE", startOfMonth, endOfMonth);
        monthTransactions.addAll(transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, "ACTIVE", startOfMonth, endOfMonth));

        BigDecimal totalIncome = monthTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = monthTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBalance = totalIncome.subtract(totalExpense);

        // Tính savingRate
        double savingRate = 0.0;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingRate = totalBalance.divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // Tính averageDailyExpense
        long daysInMonth = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        BigDecimal averageDailyExpense = BigDecimal.ZERO;
        if (daysInMonth > 0) {
            averageDailyExpense = totalExpense.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        }

        MonthOptimizedResponseDto.SummaryDto summary = new MonthOptimizedResponseDto.SummaryDto(
                totalIncome, totalExpense, totalBalance, savingRate, averageDailyExpense);

        // 3. Tính Income.topCategories
        Map<String, CategoryStats> incomeCategoryMap = new HashMap<>();
        monthTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME && t.getCategory() != null)
                .forEach(t -> {
                    String categoryName = t.getCategory().getName();
                    incomeCategoryMap.computeIfAbsent(categoryName, k -> new CategoryStats())
                            .addAmount(t.getAmount());
                });

        List<CategorySummaryDto> incomeTopCategories = incomeCategoryMap.entrySet().stream()
                .map(entry -> {
                    CategoryStats stats = entry.getValue();
                    Double pct = totalIncome.compareTo(BigDecimal.ZERO) > 0
                            ? stats.totalAmount.divide(totalIncome, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;
                    return new CategorySummaryDto(entry.getKey(), stats.totalAmount, stats.count, pct);
                })
                .sorted((a, b) -> b.getAmt().compareTo(a.getAmt()))
                .collect(Collectors.toList());

        MonthOptimizedResponseDto.IncomeDto income = new MonthOptimizedResponseDto.IncomeDto(incomeTopCategories);

        // 4. Tính Expense.topCategories
        Map<String, CategoryStats> expenseCategoryMap = new HashMap<>();
        monthTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE && t.getCategory() != null)
                .forEach(t -> {
                    String categoryName = t.getCategory().getName();
                    expenseCategoryMap.computeIfAbsent(categoryName, k -> new CategoryStats())
                            .addAmount(t.getAmount());
                });

        List<CategorySummaryDto> expenseTopCategories = expenseCategoryMap.entrySet().stream()
                .map(entry -> {
                    CategoryStats stats = entry.getValue();
                    Double pct = totalExpense.compareTo(BigDecimal.ZERO) > 0
                            ? stats.totalAmount.divide(totalExpense, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;
                    return new CategorySummaryDto(entry.getKey(), stats.totalAmount, stats.count, pct);
                })
                .sorted((a, b) -> b.getAmt().compareTo(a.getAmt()))
                .collect(Collectors.toList());

        MonthOptimizedResponseDto.ExpenseDto expense = new MonthOptimizedResponseDto.ExpenseDto(expenseTopCategories);

        // 5. Tính goals
        List<Goal> userGoals = goalRepository.findByUserId(userId);
        List<GoalSummaryDto> goals = userGoals.stream()
                .filter(g -> g.getStatus() == GoalStatus.ACTIVE)
                .map(goal -> {
                    // Tính progress
                    BigDecimal saved = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
                    BigDecimal target = goal.getAmount() != null ? goal.getAmount() : BigDecimal.ONE;
                    double prog = saved.divide(target, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();

                    // Tính days remaining
                    LocalDateTime now = LocalDateTime.now();
                    long days = ChronoUnit.DAYS.between(now.toLocalDate(), goal.getEndAt().toLocalDate());
                    if (days < 0) days = 0;

                    // Tính risk: nếu progress < 50% và còn < 30 ngày thì risk = true
                    boolean risk = prog < 50.0 && days < 30;

                    return new GoalSummaryDto(goal.getTitle(), prog, days, risk);
                })
                .collect(Collectors.toList());

        // 6. Tính trends (so sánh với tháng trước)
        BigDecimal previousMonthIncome = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.INCOME, "ACTIVE", startOfPreviousMonth, endOfPreviousMonth)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousMonthExpense = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, "ACTIVE", startOfPreviousMonth, endOfPreviousMonth)
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính phần trăm thay đổi
        double expenseChange = 0.0;
        if (previousMonthExpense.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = totalExpense.subtract(previousMonthExpense);
            expenseChange = diff.divide(previousMonthExpense, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        } else if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            expenseChange = 100.0; // Tăng từ 0
        }

        double incomeChange = 0.0;
        if (previousMonthIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = totalIncome.subtract(previousMonthIncome);
            incomeChange = diff.divide(previousMonthIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        } else if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            incomeChange = 100.0; // Tăng từ 0
        }

        TrendsDto trends = new TrendsDto(expenseChange, incomeChange);

        // Tạo response
        MonthOptimizedResponseDto response = new MonthOptimizedResponseDto();
        response.setPeriod(period);
        response.setSummary(summary);
        response.setIncome(income);
        response.setExpense(expense);
        response.setGoals(goals);
        response.setTrends(trends);

        return response;
    }

    // Helper class để tính toán category statistics
    private static class CategoryStats {
        BigDecimal totalAmount = BigDecimal.ZERO;
        long count = 0;

        void addAmount(BigDecimal amount) {
            this.totalAmount = this.totalAmount.add(amount);
            this.count++;
        }
    }
}

