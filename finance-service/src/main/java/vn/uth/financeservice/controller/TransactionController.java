package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.uth.financeservice.dto.TransactionRequestDto;
import vn.uth.financeservice.dto.TransactionResponseDto;
import vn.uth.financeservice.entity.Transaction;
import vn.uth.financeservice.client.AuthServiceClient;
import vn.uth.financeservice.service.TransactionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthServiceClient authServiceClient;

    @PostMapping
    public ResponseEntity<TransactionResponseDto> create(@RequestBody @Validated TransactionRequestDto dto) {
        UUID userId = authServiceClient.getCurrentUserId();
        Transaction transaction = transactionService.createTransaction(userId, dto);
        TransactionResponseDto response = toTransactionResponseDto(transaction);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        UUID userId = authServiceClient.getCurrentUserId();
        transactionService.deleteTransaction(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recent")
    public ResponseEntity<List<TransactionResponseDto>> getRecentTransactions(
            @RequestParam(defaultValue = "5") int limit) {
        UUID userId = authServiceClient.getCurrentUserId();
        List<TransactionResponseDto> transactions = transactionService.getRecentTransactions(userId, limit);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponseDto>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        UUID userId = authServiceClient.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        
        // Nếu không có startDate/endDate, mặc định lấy tháng hiện tại
        if (startDate == null || endDate == null) {
            LocalDateTime now = LocalDateTime.now();
            startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            endDate = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                    .withHour(23).withMinute(59).withSecond(59);
        }
        
        Page<TransactionResponseDto> transactions = transactionService.getTransactions(userId, pageable, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    private TransactionResponseDto toTransactionResponseDto(Transaction t) {
        return new TransactionResponseDto(
                t.getTransactionId(),
                t.getType(),
                t.getName(),
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getNote(),
                t.getAmount(),
                t.getTransactionDate(),
                t.getGoal() != null ? t.getGoal().getGoalId() : null
        );
    }
}

