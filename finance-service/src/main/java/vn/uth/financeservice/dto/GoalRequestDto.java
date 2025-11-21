package vn.uth.financeservice.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GoalRequestDto {
    @NotNull
    private String title;
    @NotNull
    private BigDecimal amount;
    @NotNull
    private LocalDateTime endAt;
    private LocalDateTime startAt;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
}
