package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.uth.financeservice.dto.SummaryResponseDto;
import vn.uth.financeservice.service.SummaryService;
import vn.uth.financeservice.client.AuthServiceClient;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;
    private final AuthServiceClient authServiceClient;

    @GetMapping("/test-jwt")
    public ResponseEntity<Map<String, Object>> testJwt(JwtAuthenticationToken token) {
        if (token == null) {
            return ResponseEntity.ok(Map.of("error", "No JWT token found", "authenticated", false));
        }
        
        String sub = token.getToken().getClaim("sub");
        String scope = token.getToken().getClaim("scope");
        String issuer = token.getToken().getClaim("iss");
        
        return ResponseEntity.ok(Map.of(
                "sub", sub != null ? sub : "null",
                "scope", scope != null ? scope : "null",
                "iss", issuer != null ? issuer : "null",
                "authenticated", true,
                "message", "JWT token is valid and decoded successfully"
        ));
    }

    @GetMapping("/month")
    public ResponseEntity<SummaryResponseDto> getMonthlySummary() {
        UUID userId = authServiceClient.getCurrentUserId();
        SummaryResponseDto summary = summaryService.getMonthlySummary(userId);
        return ResponseEntity.ok(summary);
    }
}


