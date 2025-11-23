package vn.uth.financeservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class GoalWithdrawRequestDto {
    @NotNull(message = "Số tiền rút không được để trống")
    @Positive(message = "Số tiền rút phải lớn hơn 0")
    private BigDecimal amount;

    private String note;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

