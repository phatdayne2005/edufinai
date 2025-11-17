package vn.uth.financeservice.dto;

import vn.uth.financeservice.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionResponseDto {
    private UUID transactionId;
    private TransactionType type; // INCOME or EXPENSE
    private String name;
    private String category;
    private String note;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private UUID goalId; // Optional: goal ID nếu transaction được gắn vào goal

    public TransactionResponseDto() {}

    public TransactionResponseDto(UUID transactionId, TransactionType type, String name, 
                                 String category, String note, BigDecimal amount, 
                                 LocalDateTime transactionDate, UUID goalId) {
        this.transactionId = transactionId;
        this.type = type;
        this.name = name;
        this.category = category;
        this.note = note;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.goalId = goalId;
    }

    // Getters and Setters
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    public UUID getGoalId() { return goalId; }
    public void setGoalId(UUID goalId) { this.goalId = goalId; }
}

