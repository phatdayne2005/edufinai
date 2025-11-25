package vn.uth.financeservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for daily report response
 * API: GET /api/summary/daily
 */
public class DailyReportResponseDto {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;
    private DailySummaryDto summary;
    private ExpenseBreakdownDto expenseBreakdown;
    private ComparisonDto comparison;
    private GoalsDto goals;

    public DailyReportResponseDto() {}

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public DailySummaryDto getSummary() {
        return summary;
    }

    public void setSummary(DailySummaryDto summary) {
        this.summary = summary;
    }

    public ExpenseBreakdownDto getExpenseBreakdown() {
        return expenseBreakdown;
    }

    public void setExpenseBreakdown(ExpenseBreakdownDto expenseBreakdown) {
        this.expenseBreakdown = expenseBreakdown;
    }

    public ComparisonDto getComparison() {
        return comparison;
    }

    public void setComparison(ComparisonDto comparison) {
        this.comparison = comparison;
    }

    public GoalsDto getGoals() {
        return goals;
    }

    public void setGoals(GoalsDto goals) {
        this.goals = goals;
    }

    // Nested Classes

    /**
     * Daily summary
     */
    public static class DailySummaryDto {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netAmount;
        private Integer transactionCount;
        private BigDecimal avgTransactionAmount;

        public DailySummaryDto() {}

        public DailySummaryDto(BigDecimal totalIncome, BigDecimal totalExpense, 
                              BigDecimal netAmount, Integer transactionCount, 
                              BigDecimal avgTransactionAmount) {
            this.totalIncome = totalIncome;
            this.totalExpense = totalExpense;
            this.netAmount = netAmount;
            this.transactionCount = transactionCount;
            this.avgTransactionAmount = avgTransactionAmount;
        }

        public BigDecimal getTotalIncome() {
            return totalIncome;
        }

        public void setTotalIncome(BigDecimal totalIncome) {
            this.totalIncome = totalIncome;
        }

        public BigDecimal getTotalExpense() {
            return totalExpense;
        }

        public void setTotalExpense(BigDecimal totalExpense) {
            this.totalExpense = totalExpense;
        }

        public BigDecimal getNetAmount() {
            return netAmount;
        }

        public void setNetAmount(BigDecimal netAmount) {
            this.netAmount = netAmount;
        }

        public Integer getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(Integer transactionCount) {
            this.transactionCount = transactionCount;
        }

        public BigDecimal getAvgTransactionAmount() {
            return avgTransactionAmount;
        }

        public void setAvgTransactionAmount(BigDecimal avgTransactionAmount) {
            this.avgTransactionAmount = avgTransactionAmount;
        }
    }

    /**
     * Expense breakdown
     */
    public static class ExpenseBreakdownDto {
        private List<CategorySummaryDto> byCategory;
        private LargestTransactionDto largestTransaction;

        public ExpenseBreakdownDto() {}

        public ExpenseBreakdownDto(List<CategorySummaryDto> byCategory, 
                                   LargestTransactionDto largestTransaction) {
            this.byCategory = byCategory;
            this.largestTransaction = largestTransaction;
        }

        public List<CategorySummaryDto> getByCategory() {
            return byCategory;
        }

        public void setByCategory(List<CategorySummaryDto> byCategory) {
            this.byCategory = byCategory;
        }

        public LargestTransactionDto getLargestTransaction() {
            return largestTransaction;
        }

        public void setLargestTransaction(LargestTransactionDto largestTransaction) {
            this.largestTransaction = largestTransaction;
        }
    }

    /**
     * Largest transaction info
     */
    public static class LargestTransactionDto {
        private String name;
        private BigDecimal amount;
        private String category;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime time;

        public LargestTransactionDto() {}

        public LargestTransactionDto(String name, BigDecimal amount, 
                                     String category, LocalDateTime time) {
            this.name = name;
            this.amount = amount;
            this.category = category;
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public LocalDateTime getTime() {
            return time;
        }

        public void setTime(LocalDateTime time) {
            this.time = time;
        }
    }

    /**
     * Comparison with previous day and 7-day average
     */
    public static class ComparisonDto {
        private PreviousDayDto previousDay;
        private Double expenseChangePct;
        private Double incomeChangePct;
        private Avg7DaysDto avg7Days;

        public ComparisonDto() {}

        public ComparisonDto(PreviousDayDto previousDay, Double expenseChangePct, 
                            Double incomeChangePct, Avg7DaysDto avg7Days) {
            this.previousDay = previousDay;
            this.expenseChangePct = expenseChangePct;
            this.incomeChangePct = incomeChangePct;
            this.avg7Days = avg7Days;
        }

        public PreviousDayDto getPreviousDay() {
            return previousDay;
        }

        public void setPreviousDay(PreviousDayDto previousDay) {
            this.previousDay = previousDay;
        }

        public Double getExpenseChangePct() {
            return expenseChangePct;
        }

        public void setExpenseChangePct(Double expenseChangePct) {
            this.expenseChangePct = expenseChangePct;
        }

        public Double getIncomeChangePct() {
            return incomeChangePct;
        }

        public void setIncomeChangePct(Double incomeChangePct) {
            this.incomeChangePct = incomeChangePct;
        }

        public Avg7DaysDto getAvg7Days() {
            return avg7Days;
        }

        public void setAvg7Days(Avg7DaysDto avg7Days) {
            this.avg7Days = avg7Days;
        }
    }

    /**
     * Previous day data
     */
    public static class PreviousDayDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private BigDecimal totalExpense;
        private BigDecimal totalIncome;

        public PreviousDayDto() {}

        public PreviousDayDto(LocalDate date, BigDecimal totalExpense, BigDecimal totalIncome) {
            this.date = date;
            this.totalExpense = totalExpense;
            this.totalIncome = totalIncome;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getTotalExpense() {
            return totalExpense;
        }

        public void setTotalExpense(BigDecimal totalExpense) {
            this.totalExpense = totalExpense;
        }

        public BigDecimal getTotalIncome() {
            return totalIncome;
        }

        public void setTotalIncome(BigDecimal totalIncome) {
            this.totalIncome = totalIncome;
        }
    }

    /**
     * 7-day average
     */
    public static class Avg7DaysDto {
        private BigDecimal expense;
        private BigDecimal income;

        public Avg7DaysDto() {}

        public Avg7DaysDto(BigDecimal expense, BigDecimal income) {
            this.expense = expense;
            this.income = income;
        }

        public BigDecimal getExpense() {
            return expense;
        }

        public void setExpense(BigDecimal expense) {
            this.expense = expense;
        }

        public BigDecimal getIncome() {
            return income;
        }

        public void setIncome(BigDecimal income) {
            this.income = income;
        }
    }

    /**
     * Goals information
     */
    public static class GoalsDto {
        private Integer activeCount;
        private BigDecimal totalSavedToday;
        private BigDecimal totalSaved7Days;
        private List<GoalProgressDto> goalsProgress;

        public GoalsDto() {}

        public GoalsDto(Integer activeCount, BigDecimal totalSavedToday, 
                       BigDecimal totalSaved7Days, List<GoalProgressDto> goalsProgress) {
            this.activeCount = activeCount;
            this.totalSavedToday = totalSavedToday;
            this.totalSaved7Days = totalSaved7Days;
            this.goalsProgress = goalsProgress;
        }

        public Integer getActiveCount() {
            return activeCount;
        }

        public void setActiveCount(Integer activeCount) {
            this.activeCount = activeCount;
        }

        public BigDecimal getTotalSavedToday() {
            return totalSavedToday;
        }

        public void setTotalSavedToday(BigDecimal totalSavedToday) {
            this.totalSavedToday = totalSavedToday;
        }

        public BigDecimal getTotalSaved7Days() {
            return totalSaved7Days;
        }

        public void setTotalSaved7Days(BigDecimal totalSaved7Days) {
            this.totalSaved7Days = totalSaved7Days;
        }

        public List<GoalProgressDto> getGoalsProgress() {
            return goalsProgress;
        }

        public void setGoalsProgress(List<GoalProgressDto> goalsProgress) {
            this.goalsProgress = goalsProgress;
        }
    }

    /**
     * Goal progress with risk indicator
     */
    public static class GoalProgressDto {
        private String title;
        private Double progressPct;
        private Long daysRemaining;
        private Boolean risk;

        public GoalProgressDto() {}

        public GoalProgressDto(String title, Double progressPct, Long daysRemaining, Boolean risk) {
            this.title = title;
            this.progressPct = progressPct;
            this.daysRemaining = daysRemaining;
            this.risk = risk;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Double getProgressPct() {
            return progressPct;
        }

        public void setProgressPct(Double progressPct) {
            this.progressPct = progressPct;
        }

        public Long getDaysRemaining() {
            return daysRemaining;
        }

        public void setDaysRemaining(Long daysRemaining) {
            this.daysRemaining = daysRemaining;
        }

        public Boolean getRisk() {
            return risk;
        }

        public void setRisk(Boolean risk) {
            this.risk = risk;
        }
    }
}

