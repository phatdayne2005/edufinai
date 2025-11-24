package vn.uth.financeservice.dto;

public class TrendsDto {
    private Double expenseChange; // percentage change from previous month
    private Double incomeChange; // percentage change from previous month

    public TrendsDto() {}

    public TrendsDto(Double expenseChange, Double incomeChange) {
        this.expenseChange = expenseChange;
        this.incomeChange = incomeChange;
    }

    public Double getExpenseChange() {
        return expenseChange;
    }

    public void setExpenseChange(Double expenseChange) {
        this.expenseChange = expenseChange;
    }

    public Double getIncomeChange() {
        return incomeChange;
    }

    public void setIncomeChange(Double incomeChange) {
        this.incomeChange = incomeChange;
    }
}

