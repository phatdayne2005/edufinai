package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.uth.financeservice.dto.BalanceInitializeRequestDto;
import vn.uth.financeservice.dto.BalanceResponseDto;
import vn.uth.financeservice.client.AuthServiceClient;
import vn.uth.financeservice.service.BalanceService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/balance")
@RequiredArgsConstructor
@Validated
public class BalanceController {

    private final BalanceService balanceService;
    private final AuthServiceClient authServiceClient;

    @PostMapping("/initialize")
    public ResponseEntity<?> initialize(@RequestBody @Validated BalanceInitializeRequestDto dto) {
        UUID userId = authServiceClient.getCurrentUserId();
        return ResponseEntity.ok(balanceService.initializeBalance(userId, dto.getAmount()));
    }

    @GetMapping
    public ResponseEntity<BalanceResponseDto> getCurrentBalance() {
        UUID userId = authServiceClient.getCurrentUserId();
        BalanceResponseDto balance = balanceService.getCurrentBalance(userId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/check-initialized")
    public ResponseEntity<Boolean> checkInitialized() {
        UUID userId = authServiceClient.getCurrentUserId();
        boolean initialized = balanceService.hasInitializedBalance(userId);
        return ResponseEntity.ok(initialized);
    }
}

