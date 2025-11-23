package vn.uth.financeservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.BalanceResponseDto;
import vn.uth.financeservice.entity.Transaction;
import vn.uth.financeservice.entity.TransactionType;
import vn.uth.financeservice.entity.UserBalance;
import vn.uth.financeservice.repository.TransactionRepository;
import vn.uth.financeservice.repository.UserBalanceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserBalanceRepository userBalanceRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public UserBalance initializeBalance(UUID userId, BigDecimal amount) {
        // Kiểm tra xem user đã có balance chưa
        UserBalance existingBalance = userBalanceRepository.findByUserId(userId).orElse(null);
        
        if (existingBalance != null) {
            throw new RuntimeException("Số dư ban đầu đã được khai báo. Không thể khai báo lại.");
        }

        // Tạo balance mới
        UserBalance balance = new UserBalance();
        balance.setUserId(userId);
        balance.setInitialBalance(amount);
        balance.setCreatedAt(LocalDateTime.now());
        balance.setUpdatedAt(LocalDateTime.now());

        return userBalanceRepository.save(balance);
    }

    @Transactional(readOnly = true)
    public BalanceResponseDto getCurrentBalance(UUID userId) {
        // Lấy initial balance (mặc định 0 nếu chưa khai báo)
        BigDecimal initialBalance = userBalanceRepository.findByUserId(userId)
                .map(UserBalance::getInitialBalance)
                .orElse(BigDecimal.ZERO);

        // Tính tổng thu nhập thông thường (INCOME không có goalId, chỉ ACTIVE)
        BigDecimal totalIncome = transactionRepository
                .findByUserIdAndTypeAndStatusAndGoalIsNull(userId, TransactionType.INCOME, "ACTIVE")
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính tổng nạp vào goal (INCOME có goalId, chỉ ACTIVE) - sẽ trừ khỏi số dư
        BigDecimal totalGoalDeposit = transactionRepository
                .findByUserIdAndTypeAndStatusAndGoalIsNotNull(userId, TransactionType.INCOME, "ACTIVE")
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính tổng chi tiêu (chỉ ACTIVE)
        BigDecimal totalExpense = transactionRepository
                .findByUserIdAndTypeAndStatus(userId, TransactionType.EXPENSE, "ACTIVE")
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính tổng rút tiền từ goal (chỉ ACTIVE) - sẽ cộng vào số dư
        BigDecimal totalWithdrawal = transactionRepository
                .findByUserIdAndTypeAndStatus(userId, TransactionType.WITHDRAWAL, "ACTIVE")
                .stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Số dư hiện tại = initialBalance + totalIncome - totalExpense - totalGoalDeposit + totalWithdrawal
        // Logic: Nạp vào goal trừ khỏi số dư, rút từ goal cộng vào số dư
        BigDecimal currentBalance = initialBalance
                .add(totalIncome)           // Thu nhập thông thường: cộng
                .subtract(totalExpense)     // Chi tiêu: trừ
                .subtract(totalGoalDeposit)  // Nạp vào goal: trừ (tiền bị khóa)
                .add(totalWithdrawal);      // Rút từ goal: cộng (tiền được giải phóng)

        return new BalanceResponseDto(
                currentBalance,
                initialBalance,
                totalIncome,
                totalGoalDeposit,
                totalExpense,
                totalWithdrawal
        );
    }

    @Transactional(readOnly = true)
    public boolean hasInitializedBalance(UUID userId) {
        return userBalanceRepository.findByUserId(userId).isPresent();
    }
}

