package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<?> create(@RequestBody @Validated TransactionRequestDto dto,
                                    Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(transactionService.createTransaction(userId, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        transactionService.deleteTransaction(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recent")
    public ResponseEntity<List<TransactionResponseDto>> getRecentTransactions(
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<TransactionResponseDto> transactions = transactionService.getRecentTransactions(userId, limit);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponseDto>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        UUID userId = extractUserId(authentication);
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

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Unauthenticated request");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID uuid) {
            return uuid;
        }
        if (principal instanceof String value) {
            return UUID.fromString(value);
        }
        throw new RuntimeException("Invalid authentication principal");
    }
}

