package vn.uth.financeservice.dto;

import java.math.BigDecimal;

public class BalanceResponseDto {
    private BigDecimal currentBalance;
    private BigDecimal initialBalance;
    private BigDecimal totalIncome; // Thu nhập thông thường (không có goalId)
    private BigDecimal totalGoalDeposit; // Tổng nạp vào goal (INCOME có goalId)
    private BigDecimal totalExpense;
    private BigDecimal totalWithdrawal; // Tổng rút từ goal (cộng vào số dư)

    public BalanceResponseDto() {}

    public BalanceResponseDto(BigDecimal currentBalance, BigDecimal initialBalance, 
                             BigDecimal totalIncome, BigDecimal totalGoalDeposit,
                             BigDecimal totalExpense, BigDecimal totalWithdrawal) {
        this.currentBalance = currentBalance;
        this.initialBalance = initialBalance;
        this.totalIncome = totalIncome;
        this.totalGoalDeposit = totalGoalDeposit;
        this.totalExpense = totalExpense;
        this.totalWithdrawal = totalWithdrawal;
    }

    // Getters and Setters
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
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

    public BigDecimal getTotalWithdrawal() {
        return totalWithdrawal;
    }

    public void setTotalWithdrawal(BigDecimal totalWithdrawal) {
        this.totalWithdrawal = totalWithdrawal;
    }

    public BigDecimal getTotalGoalDeposit() {
        return totalGoalDeposit;
    }

    public void setTotalGoalDeposit(BigDecimal totalGoalDeposit) {
        this.totalGoalDeposit = totalGoalDeposit;
    }
}

