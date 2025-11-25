package vn.uth.financeservice.dto;

import vn.uth.financeservice.entity.CategoryType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Category Transactions API response
 * Trả về danh sách transactions của một category trong một khoảng thời gian
 */
public class CategoryTransactionsDto {
    private UUID categoryId;
    private String categoryName;
    private CategoryType categoryType; // INCOME, EXPENSE, BOTH
    private PeriodInfo period;
    private TransactionSummary summary;
    private List<TransactionResponseDto> transactions;

    public CategoryTransactionsDto() {}

    public CategoryTransactionsDto(UUID categoryId, String categoryName, CategoryType categoryType,
                                   PeriodInfo period, TransactionSummary summary,
                                   List<TransactionResponseDto> transactions) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryType = categoryType;
        this.period = period;
        this.summary = summary;
        this.transactions = transactions;
    }

    // Getters and Setters
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public CategoryType getCategoryType() { return categoryType; }
    public void setCategoryType(CategoryType categoryType) { this.categoryType = categoryType; }

    public PeriodInfo getPeriod() { return period; }
    public void setPeriod(PeriodInfo period) { this.period = period; }

    public TransactionSummary getSummary() { return summary; }
    public void setSummary(TransactionSummary summary) { this.summary = summary; }

    public List<TransactionResponseDto> getTransactions() { return transactions; }
    public void setTransactions(List<TransactionResponseDto> transactions) { this.transactions = transactions; }

    /**
     * Nested class: Thông tin khoảng thời gian
     */
    public static class PeriodInfo {
        private Integer month;
        private Integer year;
        private LocalDate startDate;
        private LocalDate endDate;

        public PeriodInfo() {}

        public PeriodInfo(Integer month, Integer year, LocalDate startDate, LocalDate endDate) {
            this.month = month;
            this.year = year;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        // Getters and Setters
        public Integer getMonth() { return month; }
        public void setMonth(Integer month) { this.month = month; }

        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    /**
     * Nested class: Tổng hợp thông tin transactions
     */
    public static class TransactionSummary {
        private BigDecimal totalAmount;
        private Integer transactionCount;
        private BigDecimal averageAmount;

        public TransactionSummary() {}

        public TransactionSummary(BigDecimal totalAmount, Integer transactionCount, BigDecimal averageAmount) {
            this.totalAmount = totalAmount;
            this.transactionCount = transactionCount;
            this.averageAmount = averageAmount;
        }

        // Getters and Setters
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public Integer getTransactionCount() { return transactionCount; }
        public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }

        public BigDecimal getAverageAmount() { return averageAmount; }
        public void setAverageAmount(BigDecimal averageAmount) { this.averageAmount = averageAmount; }
    }
}

