package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<SummaryResponseDto> getMonthlySummary() {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes()); // sau này sẽ thay bằng JWT
        SummaryResponseDto summary = summaryService.getMonthlySummary(userId);
        return ResponseEntity.ok(summary);
    }
}


