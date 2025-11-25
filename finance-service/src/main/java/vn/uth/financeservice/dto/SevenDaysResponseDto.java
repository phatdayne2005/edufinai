package vn.uth.financeservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for 7-day summary response
 * API: GET /api/summary/7days
 */
public class SevenDaysResponseDto {
    private SevenDaysPeriodDto period;
    private SevenDaysSummaryDto summary;
    private SevenDaysExpenseDto expense;
    private SevenDaysIncomeDto income;
    private List<SevenDaysGoalDto> goals;

    public SevenDaysResponseDto() {}

    public SevenDaysPeriodDto getPeriod() {
        return period;
    }

    public void setPeriod(SevenDaysPeriodDto period) {
        this.period = period;
    }

    public SevenDaysSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(SevenDaysSummaryDto summary) {
        this.summary = summary;
    }

    public SevenDaysExpenseDto getExpense() {
        return expense;
    }

    public void setExpense(SevenDaysExpenseDto expense) {
        this.expense = expense;
    }

    public SevenDaysIncomeDto getIncome() {
        return income;
    }

    public void setIncome(SevenDaysIncomeDto income) {
        this.income = income;
    }

    public List<SevenDaysGoalDto> getGoals() {
        return goals;
    }

    public void setGoals(List<SevenDaysGoalDto> goals) {
        this.goals = goals;
    }

    // Nested Classes

    /**
     * Period information with days count
     */
    public static class SevenDaysPeriodDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
        
        private Integer days;

        public SevenDaysPeriodDto() {}

        public SevenDaysPeriodDto(LocalDate startDate, LocalDate endDate, Integer days) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.days = days;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public Integer getDays() {
            return days;
        }

        public void setDays(Integer days) {
            this.days = days;
        }
    }

    /**
     * Summary with averageDailyIncome
     */
    public static class SevenDaysSummaryDto {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal totalBalance;
        private Double savingRate;
        private BigDecimal averageDailyExpense;
        private BigDecimal averageDailyIncome;

        public SevenDaysSummaryDto() {}

        public SevenDaysSummaryDto(BigDecimal totalIncome, BigDecimal totalExpense, 
                                   BigDecimal totalBalance, Double savingRate, 
                                   BigDecimal averageDailyExpense, BigDecimal averageDailyIncome) {
            this.totalIncome = totalIncome;
            this.totalExpense = totalExpense;
            this.totalBalance = totalBalance;
            this.savingRate = savingRate;
            this.averageDailyExpense = averageDailyExpense;
            this.averageDailyIncome = averageDailyIncome;
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

        public BigDecimal getAverageDailyIncome() {
            return averageDailyIncome;
        }

        public void setAverageDailyIncome(BigDecimal averageDailyIncome) {
            this.averageDailyIncome = averageDailyIncome;
        }
    }

    /**
     * Expense with topCategories and dailyBreakdown
     */
    public static class SevenDaysExpenseDto {
        private List<CategorySummaryDto> topCategories;
        private List<DailyBreakdownDto> dailyBreakdown;

        public SevenDaysExpenseDto() {}

        public SevenDaysExpenseDto(List<CategorySummaryDto> topCategories, List<DailyBreakdownDto> dailyBreakdown) {
            this.topCategories = topCategories;
            this.dailyBreakdown = dailyBreakdown;
        }

        public List<CategorySummaryDto> getTopCategories() {
            return topCategories;
        }

        public void setTopCategories(List<CategorySummaryDto> topCategories) {
            this.topCategories = topCategories;
        }

        public List<DailyBreakdownDto> getDailyBreakdown() {
            return dailyBreakdown;
        }

        public void setDailyBreakdown(List<DailyBreakdownDto> dailyBreakdown) {
            this.dailyBreakdown = dailyBreakdown;
        }
    }

    /**
     * Daily breakdown for expenses
     */
    public static class DailyBreakdownDto {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private BigDecimal total;
        private Long count;

        public DailyBreakdownDto() {}

        public DailyBreakdownDto(LocalDate date, BigDecimal total, Long count) {
            this.date = date;
            this.total = total;
            this.count = count;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }

    /**
     * Income with topSources (renamed from topCategories)
     */
    public static class SevenDaysIncomeDto {
        private List<IncomeSummaryDto> topSources;

        public SevenDaysIncomeDto() {}

        public SevenDaysIncomeDto(List<IncomeSummaryDto> topSources) {
            this.topSources = topSources;
        }

        public List<IncomeSummaryDto> getTopSources() {
            return topSources;
        }

        public void setTopSources(List<IncomeSummaryDto> topSources) {
            this.topSources = topSources;
        }
    }

    /**
     * Income source summary (similar to CategorySummaryDto but with "source" field)
     */
    public static class IncomeSummaryDto {
        private String source; // category name for income
        private BigDecimal amt; // amount
        private Long cnt; // count
        private Double pct; // percent

        public IncomeSummaryDto() {}

        public IncomeSummaryDto(String source, BigDecimal amt, Long cnt, Double pct) {
            this.source = source;
            this.amt = amt;
            this.cnt = cnt;
            this.pct = pct;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public BigDecimal getAmt() {
            return amt;
        }

        public void setAmt(BigDecimal amt) {
            this.amt = amt;
        }

        public Long getCnt() {
            return cnt;
        }

        public void setCnt(Long cnt) {
            this.cnt = cnt;
        }

        public Double getPct() {
            return pct;
        }

        public void setPct(Double pct) {
            this.pct = pct;
        }
    }

    /**
     * Goal summary for 7-day report (simplified, no risk field)
     */
    public static class SevenDaysGoalDto {
        private String title;
        private Double progressPct;
        private Long daysRemaining;

        public SevenDaysGoalDto() {}

        public SevenDaysGoalDto(String title, Double progressPct, Long daysRemaining) {
            this.title = title;
            this.progressPct = progressPct;
            this.daysRemaining = daysRemaining;
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
    }
}

