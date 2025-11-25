package vn.uth.financeservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class GoalTransactionHistoryDto {
    private String goalTitle;
    private BigDecimal goalAmount;
    private BigDecimal savedAmount;
    private List<TransactionResponseDto> transactions;
    private TransactionSummary summary;

    public GoalTransactionHistoryDto() {}

    public GoalTransactionHistoryDto(String goalTitle, BigDecimal goalAmount, 
                                    BigDecimal savedAmount, List<TransactionResponseDto> transactions,
                                    TransactionSummary summary) {
        this.goalTitle = goalTitle;
        this.goalAmount = goalAmount;
        this.savedAmount = savedAmount;
        this.transactions = transactions;
        this.summary = summary;
    }

    // Getters and Setters
    public String getGoalTitle() {
        return goalTitle;
    }

    public void setGoalTitle(String goalTitle) {
        this.goalTitle = goalTitle;
    }

    public BigDecimal getGoalAmount() {
        return goalAmount;
    }

    public void setGoalAmount(BigDecimal goalAmount) {
        this.goalAmount = goalAmount;
    }

    public BigDecimal getSavedAmount() {
        return savedAmount;
    }

    public void setSavedAmount(BigDecimal savedAmount) {
        this.savedAmount = savedAmount;
    }

    public List<TransactionResponseDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionResponseDto> transactions) {
        this.transactions = transactions;
    }

    public TransactionSummary getSummary() {
        return summary;
    }

    public void setSummary(TransactionSummary summary) {
        this.summary = summary;
    }

    // Inner class for summary
    public static class TransactionSummary {
        private BigDecimal totalDeposit;
        private BigDecimal totalWithdrawal;
        private Integer transactionCount;

        public TransactionSummary() {}

        public TransactionSummary(BigDecimal totalDeposit, BigDecimal totalWithdrawal, Integer transactionCount) {
            this.totalDeposit = totalDeposit;
            this.totalWithdrawal = totalWithdrawal;
            this.transactionCount = transactionCount;
        }

        public BigDecimal getTotalDeposit() {
            return totalDeposit;
        }

        public void setTotalDeposit(BigDecimal totalDeposit) {
            this.totalDeposit = totalDeposit;
        }

        public BigDecimal getTotalWithdrawal() {
            return totalWithdrawal;
        }

        public void setTotalWithdrawal(BigDecimal totalWithdrawal) {
            this.totalWithdrawal = totalWithdrawal;
        }

        public Integer getTransactionCount() {
            return transactionCount;
        }

        public void setTransactionCount(Integer transactionCount) {
            this.transactionCount = transactionCount;
        }
    }
}

