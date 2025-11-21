package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.uth.financeservice.dto.SummaryResponseDto;
import vn.uth.financeservice.service.SummaryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/month")
    public ResponseEntity<SummaryResponseDto> getMonthlySummary(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        SummaryResponseDto summary = summaryService.getMonthlySummary(userId);
        return ResponseEntity.ok(summary);
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


