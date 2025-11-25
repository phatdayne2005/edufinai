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

    /**
     * API: GET /api/summary/7days
     * Lấy tổng hợp tài chính 7 ngày gần nhất
     * 
     * @param userId UUID của user (từ JWT)
     * @return SevenDaysResponseDto
     */
    @Transactional(readOnly = true)
    public SevenDaysResponseDto get7DaysSummary(UUID userId) {
        // 1. Tính period: 7 ngày gần nhất (từ hôm nay - 6 ngày đến hôm nay)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        LocalDateTime startOfPeriod = startDate.atStartOfDay();
        LocalDateTime endOfPeriod = endDate.atTime(23, 59, 59);
        int days = 7;

        SevenDaysResponseDto.SevenDaysPeriodDto period = 
            new SevenDaysResponseDto.SevenDaysPeriodDto(startDate, endDate, days);

        // 2. Lấy tất cả transactions trong 7 ngày (ACTIVE)
        List<Transaction> incomeTransactions = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.INCOME, "ACTIVE", startOfPeriod, endOfPeriod);
        
        List<Transaction> expenseTransactions = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, "ACTIVE", startOfPeriod, endOfPeriod);

        // 3. Tính summary
        BigDecimal totalIncome = incomeTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = expenseTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // totalBalance = lấy từ BalanceService (số dư thực tế hiện tại)
        BigDecimal totalBalance = balanceService.getCurrentBalance(userId).getCurrentBalance();

        // Tính savingRate: (totalIncome - totalExpense) / totalIncome * 100
        double savingRate = 0.0;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal savings = totalIncome.subtract(totalExpense);
            savingRate = savings.divide(totalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // Tính averageDailyExpense: totalExpense / 7
        BigDecimal averageDailyExpense = totalExpense.divide(
                BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);

        // Tính averageDailyIncome: totalIncome / 7
        BigDecimal averageDailyIncome = totalIncome.divide(
                BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);

        SevenDaysResponseDto.SevenDaysSummaryDto summary = 
            new SevenDaysResponseDto.SevenDaysSummaryDto(
                totalIncome, totalExpense, totalBalance, savingRate, 
                averageDailyExpense, averageDailyIncome);

        // 4. Tính Expense.topCategories
        Map<String, CategoryStats> expenseCategoryMap = new HashMap<>();
        expenseTransactions.stream()
                .filter(t -> t.getCategory() != null)
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

        // 5. Tính Expense.dailyBreakdown (chi tiêu theo từng ngày, sắp xếp mới nhất trước)
        Map<LocalDate, DailyStats> dailyExpenseMap = new HashMap<>();
        expenseTransactions.forEach(t -> {
            LocalDate date = t.getTransactionDate().toLocalDate();
            dailyExpenseMap.computeIfAbsent(date, k -> new DailyStats())
                    .addAmount(t.getAmount());
        });

        List<SevenDaysResponseDto.DailyBreakdownDto> dailyBreakdown = dailyExpenseMap.entrySet().stream()
                .map(entry -> new SevenDaysResponseDto.DailyBreakdownDto(
                        entry.getKey(), 
                        entry.getValue().totalAmount, 
                        entry.getValue().count))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate())) // Mới nhất trước
                .collect(Collectors.toList());

        SevenDaysResponseDto.SevenDaysExpenseDto expense = 
            new SevenDaysResponseDto.SevenDaysExpenseDto(expenseTopCategories, dailyBreakdown);

        // 6. Tính Income.topSources
        Map<String, CategoryStats> incomeCategoryMap = new HashMap<>();
        incomeTransactions.stream()
                .filter(t -> t.getCategory() != null)
                .forEach(t -> {
                    String categoryName = t.getCategory().getName();
                    incomeCategoryMap.computeIfAbsent(categoryName, k -> new CategoryStats())
                            .addAmount(t.getAmount());
                });

        List<SevenDaysResponseDto.IncomeSummaryDto> incomeTopSources = incomeCategoryMap.entrySet().stream()
                .map(entry -> {
                    CategoryStats stats = entry.getValue();
                    Double pct = totalIncome.compareTo(BigDecimal.ZERO) > 0
                            ? stats.totalAmount.divide(totalIncome, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue()
                            : 0.0;
                    return new SevenDaysResponseDto.IncomeSummaryDto(
                            entry.getKey(), stats.totalAmount, stats.count, pct);
                })
                .sorted((a, b) -> b.getAmt().compareTo(a.getAmt()))
                .collect(Collectors.toList());

        SevenDaysResponseDto.SevenDaysIncomeDto income = 
            new SevenDaysResponseDto.SevenDaysIncomeDto(incomeTopSources);

        // 7. Tính goals (chỉ ACTIVE goals)
        List<Goal> userGoals = goalRepository.findByUserId(userId);
        List<SevenDaysResponseDto.SevenDaysGoalDto> goals = userGoals.stream()
                .filter(g -> g.getStatus() == GoalStatus.ACTIVE)
                .map(goal -> {
                    // Tính progress percentage
                    BigDecimal saved = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
                    BigDecimal target = goal.getAmount() != null ? goal.getAmount() : BigDecimal.ONE;
                    double progressPct = saved.divide(target, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();

                    // Tính days remaining
                    LocalDateTime now = LocalDateTime.now();
                    long daysRemaining = ChronoUnit.DAYS.between(now.toLocalDate(), goal.getEndAt().toLocalDate());
                    if (daysRemaining < 0) daysRemaining = 0;

                    return new SevenDaysResponseDto.SevenDaysGoalDto(
                            goal.getTitle(), progressPct, daysRemaining);
                })
                .collect(Collectors.toList());

        // 8. Tạo response
        SevenDaysResponseDto response = new SevenDaysResponseDto();
        response.setPeriod(period);
        response.setSummary(summary);
        response.setExpense(expense);
        response.setIncome(income);
        response.setGoals(goals);

        return response;
    }

    /**
     * API: GET /api/summary/daily
     * Lấy báo cáo tài chính theo ngày (hôm nay)
     * 
     * @param userId UUID của user (từ JWT)
     * @return DailyReportResponseDto
     */
    @Transactional(readOnly = true)
    public DailyReportResponseDto getDailyReport(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(23, 59, 59);

        // Previous day
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        LocalDateTime endOfYesterday = yesterday.atTime(23, 59, 59);

        // 7 days ago
        LocalDate sevenDaysAgo = today.minusDays(6);
        LocalDateTime startOf7Days = sevenDaysAgo.atStartOfDay();

        // 1. Lấy tất cả transactions hôm nay
        List<Transaction> todayIncomeTransactions = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.INCOME, "ACTIVE", startOfToday, endOfToday);

        List<Transaction> todayExpenseTransactions = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, "ACTIVE", startOfToday, endOfToday);

        // 2. Tính summary
        BigDecimal totalIncome = todayIncomeTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = todayExpenseTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netAmount = totalIncome.subtract(totalExpense);

        int transactionCount = todayIncomeTransactions.size() + todayExpenseTransactions.size();

        BigDecimal avgTransactionAmount = BigDecimal.ZERO;
        if (transactionCount > 0) {
            BigDecimal totalAmount = totalIncome.add(totalExpense);
            avgTransactionAmount = totalAmount.divide(
                    BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP);
        }

        DailyReportResponseDto.DailySummaryDto summary = 
            new DailyReportResponseDto.DailySummaryDto(
                totalIncome, totalExpense, netAmount, transactionCount, avgTransactionAmount);

        // 3. Tính expenseBreakdown
        // 3.1 By category
        Map<String, CategoryStats> expenseCategoryMap = new HashMap<>();
        todayExpenseTransactions.stream()
                .filter(t -> t.getCategory() != null)
                .forEach(t -> {
                    String categoryName = t.getCategory().getName();
                    expenseCategoryMap.computeIfAbsent(categoryName, k -> new CategoryStats())
                            .addAmount(t.getAmount());
                });

        List<CategorySummaryDto> byCategory = expenseCategoryMap.entrySet().stream()
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

        // 3.2 Largest transaction
        DailyReportResponseDto.LargestTransactionDto largestTransaction = null;
        if (!todayExpenseTransactions.isEmpty()) {
            Transaction largest = todayExpenseTransactions.stream()
                    .max((t1, t2) -> t1.getAmount().compareTo(t2.getAmount()))
                    .orElse(null);
            if (largest != null) {
                largestTransaction = new DailyReportResponseDto.LargestTransactionDto(
                        largest.getName(),
                        largest.getAmount(),
                        largest.getCategory() != null ? largest.getCategory().getName() : null,
                        largest.getTransactionDate()
                );
            }
        }

        DailyReportResponseDto.ExpenseBreakdownDto expenseBreakdown = 
            new DailyReportResponseDto.ExpenseBreakdownDto(byCategory, largestTransaction);

        // 4. Tính comparison
        // 4.1 Previous day
        List<Transaction> yesterdayIncome = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.INCOME, "ACTIVE", startOfYesterday, endOfYesterday);

        List<Transaction> yesterdayExpense = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, "ACTIVE", startOfYesterday, endOfYesterday);

        BigDecimal yesterdayTotalIncome = yesterdayIncome.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yesterdayTotalExpense = yesterdayExpense.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DailyReportResponseDto.PreviousDayDto previousDay = 
            new DailyReportResponseDto.PreviousDayDto(yesterday, yesterdayTotalExpense, yesterdayTotalIncome);

        // 4.2 Expense change %
        double expenseChangePct = 0.0;
        if (yesterdayTotalExpense.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = totalExpense.subtract(yesterdayTotalExpense);
            expenseChangePct = diff.divide(yesterdayTotalExpense, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        } else if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            expenseChangePct = 100.0;
        }

        // 4.3 Income change %
        double incomeChangePct = 0.0;
        if (yesterdayTotalIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = totalIncome.subtract(yesterdayTotalIncome);
            incomeChangePct = diff.divide(yesterdayTotalIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        } else if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            incomeChangePct = 100.0;
        }

        // 4.4 7-day average
        List<Transaction> last7DaysIncome = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.INCOME, "ACTIVE", startOf7Days, endOfToday);

        List<Transaction> last7DaysExpense = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, "ACTIVE", startOf7Days, endOfToday);

        BigDecimal total7DaysIncome = last7DaysIncome.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total7DaysExpense = last7DaysExpense.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avg7DaysIncome = total7DaysIncome.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
        BigDecimal avg7DaysExpense = total7DaysExpense.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);

        DailyReportResponseDto.Avg7DaysDto avg7Days = 
            new DailyReportResponseDto.Avg7DaysDto(avg7DaysExpense, avg7DaysIncome);

        DailyReportResponseDto.ComparisonDto comparison = 
            new DailyReportResponseDto.ComparisonDto(previousDay, expenseChangePct, incomeChangePct, avg7Days);

        // 5. Tính goals
        List<Goal> allGoals = goalRepository.findByUserId(userId);
        
        // 5.1 Active count
        int activeCount = (int) allGoals.stream()
                .filter(g -> g.getStatus() == GoalStatus.ACTIVE)
                .count();

        // 5.2 Total saved today (INCOME transactions with goalId today)
        List<Transaction> todayGoalDeposits = todayIncomeTransactions.stream()
                .filter(t -> t.getGoal() != null)
                .collect(Collectors.toList());

        BigDecimal totalSavedToday = todayGoalDeposits.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5.3 Total saved last 7 days
        List<Transaction> last7DaysGoalDeposits = last7DaysIncome.stream()
                .filter(t -> t.getGoal() != null)
                .collect(Collectors.toList());

        BigDecimal totalSaved7Days = last7DaysGoalDeposits.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5.4 Goals progress
        List<DailyReportResponseDto.GoalProgressDto> goalsProgress = allGoals.stream()
                .filter(g -> g.getStatus() == GoalStatus.ACTIVE)
                .map(goal -> {
                    BigDecimal saved = goal.getSavedAmount() != null ? goal.getSavedAmount() : BigDecimal.ZERO;
                    BigDecimal target = goal.getAmount() != null ? goal.getAmount() : BigDecimal.ONE;
                    double progressPct = saved.divide(target, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();

                    LocalDateTime now = LocalDateTime.now();
                    long daysRemaining = ChronoUnit.DAYS.between(now.toLocalDate(), goal.getEndAt().toLocalDate());
                    if (daysRemaining < 0) daysRemaining = 0;

                    boolean risk = progressPct < 50.0 && daysRemaining < 30;

                    return new DailyReportResponseDto.GoalProgressDto(
                            goal.getTitle(), progressPct, daysRemaining, risk);
                })
                .collect(Collectors.toList());

        DailyReportResponseDto.GoalsDto goals = 
            new DailyReportResponseDto.GoalsDto(activeCount, totalSavedToday, totalSaved7Days, goalsProgress);

        // 6. Tạo response
        DailyReportResponseDto response = new DailyReportResponseDto();
        response.setReportDate(today);
        response.setSummary(summary);
        response.setExpenseBreakdown(expenseBreakdown);
        response.setComparison(comparison);
        response.setGoals(goals);

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

    // Helper class để tính toán daily statistics
    private static class DailyStats {
        BigDecimal totalAmount = BigDecimal.ZERO;
        long count = 0;

        void addAmount(BigDecimal amount) {
            this.totalAmount = this.totalAmount.add(amount);
            this.count++;
        }
    }
}

