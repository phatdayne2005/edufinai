package vn.uth.financeservice.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

import java.util.UUID;

public class ExpenseRequestDto {
    @NotNull
    private String type; // INCOME or EXPENSE
    @NotNull
    private BigDecimal amount;
    @NotNull
    private String category;
    private String note;
    private String linkedAccount;
    private UUID goalId;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getLinkedAccount() { return linkedAccount; }
    public void setLinkedAccount(String linkedAccount) { this.linkedAccount = linkedAccount; }
    public UUID getGoalId() { return goalId; }
    public void setGoalId(UUID goalId) { this.goalId = goalId; }
}
