package vn.uth.financeservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class MonthOptimizedResponseDto {
    private PeriodDto period;
    private SummaryDto summary;
    private IncomeDto Income;
    private ExpenseDto Expense;
    private List<GoalSummaryDto> goals;
    private TrendsDto trends;

    public MonthOptimizedResponseDto() {}

    public PeriodDto getPeriod() {
        return period;
    }

    public void setPeriod(PeriodDto period) {
        this.period = period;
    }

    public SummaryDto getSummary() {
        return summary;
    }

    public void setSummary(SummaryDto summary) {
        this.summary = summary;
    }

    public IncomeDto getIncome() {
        return Income;
    }

    public void setIncome(IncomeDto income) {
        Income = income;
    }

    public ExpenseDto getExpense() {
        return Expense;
    }

    public void setExpense(ExpenseDto expense) {
        Expense = expense;
    }

    public List<GoalSummaryDto> getGoals() {
        return goals;
    }

    public void setGoals(List<GoalSummaryDto> goals) {
        this.goals = goals;
    }

    public TrendsDto getTrends() {
        return trends;
    }

    public void setTrends(TrendsDto trends) {
        this.trends = trends;
    }

    // Inner classes for nested structure
    public static class SummaryDto {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal totalBalance;
        private Double savingRate;
        private BigDecimal averageDailyExpense;

        public SummaryDto() {}

        public SummaryDto(BigDecimal totalIncome, BigDecimal totalExpense, 
                         BigDecimal totalBalance, Double savingRate, BigDecimal averageDailyExpense) {
            this.totalIncome = totalIncome;
            this.totalExpense = totalExpense;
            this.totalBalance = totalBalance;
            this.savingRate = savingRate;
            this.averageDailyExpense = averageDailyExpense;
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

        public BigDecimal getTotalBalance() {
            return totalBalance;
        }

        public void setTotalBalance(BigDecimal totalBalance) {
            this.totalBalance = totalBalance;
        }

        public Double getSavingRate() {
            return savingRate;
        }

        public void setSavingRate(Double savingRate) {
            this.savingRate = savingRate;
        }

        public BigDecimal getAverageDailyExpense() {
            return averageDailyExpense;
        }

        public void setAverageDailyExpense(BigDecimal averageDailyExpense) {
            this.averageDailyExpense = averageDailyExpense;
        }
    }

    public static class IncomeDto {
        private List<CategorySummaryDto> topCategories;

        public IncomeDto() {}

        public IncomeDto(List<CategorySummaryDto> topCategories) {
            this.topCategories = topCategories;
        }

        public List<CategorySummaryDto> getTopCategories() {
            return topCategories;
        }

        public void setTopCategories(List<CategorySummaryDto> topCategories) {
            this.topCategories = topCategories;
        }
    }

    public static class ExpenseDto {
        private List<CategorySummaryDto> topCategories;

        public ExpenseDto() {}

        public ExpenseDto(List<CategorySummaryDto> topCategories) {
            this.topCategories = topCategories;
        }

        public List<CategorySummaryDto> getTopCategories() {
            return topCategories;
        }

        public void setTopCategories(List<CategorySummaryDto> topCategories) {
            this.topCategories = topCategories;
        }
    }
}

