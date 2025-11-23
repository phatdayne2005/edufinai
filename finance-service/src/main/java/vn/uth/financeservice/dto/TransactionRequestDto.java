package vn.uth.financeservice.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionRequestDto {
    @NotNull
    private String type; // INCOME or EXPENSE
    @NotNull
    private BigDecimal amount;
    @NotNull
    private String name; // Tên giao dịch
    private UUID categoryId; // Category ID - Optional khi có goalId, bắt buộc khi không có goalId
    private String note;
    private UUID goalId; // Optional: gắn transaction vào goal
    private LocalDateTime transactionDate; // Ngày giao dịch (nếu null thì dùng hiện tại)

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public UUID getGoalId() { return goalId; }
    public void setGoalId(UUID goalId) { this.goalId = goalId; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
}


