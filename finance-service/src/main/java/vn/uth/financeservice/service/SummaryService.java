package vn.uth.financeservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.SummaryResponseDto;
import vn.uth.financeservice.entity.TransactionType;
import vn.uth.financeservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public SummaryResponseDto getMonthlySummary(UUID userId) {
        // Lấy tháng hiện tại
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Tính tổng thu nhập trong tháng (chỉ ACTIVE)
        BigDecimal monthlyIncome = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.INCOME, "ACTIVE", startOfMonth, endOfMonth)
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính tổng chi tiêu trong tháng (chỉ ACTIVE)
        BigDecimal monthlyExpense = transactionRepository
                .findByUserIdAndTypeAndStatusAndTransactionDateBetween(
                        userId, TransactionType.EXPENSE, "ACTIVE", startOfMonth, endOfMonth)
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Số dư hiện tại = tổng thu nhập - tổng chi tiêu (tất cả thời gian, chỉ ACTIVE)
        BigDecimal totalIncome = transactionRepository
                .findByUserIdAndTypeAndStatus(userId, TransactionType.INCOME, "ACTIVE")
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactionRepository
                .findByUserIdAndTypeAndStatus(userId, TransactionType.EXPENSE, "ACTIVE")
                .stream()
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal currentBalance = totalIncome.subtract(totalExpense);

        // Tỷ lệ tiết kiệm = (số dư / thu nhập) * 100
        double savingRate = 0.0;
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balance = monthlyIncome.subtract(monthlyExpense);
            savingRate = balance.divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        return new SummaryResponseDto(currentBalance, monthlyIncome, monthlyExpense, savingRate);
    }
}

