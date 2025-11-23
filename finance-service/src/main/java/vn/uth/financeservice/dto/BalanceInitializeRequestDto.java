package vn.uth.financeservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class BalanceInitializeRequestDto {
    @NotNull(message = "Số dư ban đầu không được để trống")
    @Positive(message = "Số dư ban đầu phải lớn hơn 0")
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

