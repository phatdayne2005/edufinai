package vn.uth.financeservice.dto;

import java.math.BigDecimal;

public class SummaryResponseDto {
    private BigDecimal currentBalance;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private double savingRate; // Tỷ lệ tiết kiệm (%)

    public SummaryResponseDto() {}

    public SummaryResponseDto(BigDecimal currentBalance, BigDecimal monthlyIncome, 
                             BigDecimal monthlyExpense, double savingRate) {
        this.currentBalance = currentBalance;
        this.monthlyIncome = monthlyIncome;
        this.monthlyExpense = monthlyExpense;
        this.savingRate = savingRate;
    }

    // Getters and Setters
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }

    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public BigDecimal getMonthlyExpense() { return monthlyExpense; }
    public void setMonthlyExpense(BigDecimal monthlyExpense) { this.monthlyExpense = monthlyExpense; }

    public double getSavingRate() { return savingRate; }
    public void setSavingRate(double savingRate) { this.savingRate = savingRate; }
}


