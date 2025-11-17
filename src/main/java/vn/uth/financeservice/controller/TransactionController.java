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

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Validated TransactionRequestDto dto) {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes()); // sau này sẽ thay bằng JWT
        return ResponseEntity.ok(transactionService.createTransaction(userId, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recent")
    public ResponseEntity<List<TransactionResponseDto>> getRecentTransactions(
            @RequestParam(defaultValue = "5") int limit) {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes());
        List<TransactionResponseDto> transactions = transactionService.getRecentTransactions(userId, limit);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponseDto>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes());
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
}

