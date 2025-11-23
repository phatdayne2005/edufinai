package vn.uth.financeservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.client.AuthServiceClient;
import vn.uth.financeservice.dto.BalanceInitializeRequestDto;
import vn.uth.financeservice.repository.UserBalanceRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BalanceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @MockitoBean
    private AuthServiceClient authServiceClient;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        // Mock AuthServiceClient
        when(authServiceClient.getCurrentUserId()).thenReturn(testUserId);
    }

    @Test
    @WithMockUser
    void testInitializeBalance_Success() throws Exception {
        // Given
        BalanceInitializeRequestDto request = new BalanceInitializeRequestDto();
        request.setAmount(new BigDecimal("10000000"));

        // When & Then
        mockMvc.perform(post("/api/v1/balance/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.initialBalance").value(10000000));
    }

    @Test
    @WithMockUser
    void testInitializeBalance_Duplicate_ReturnsError() throws Exception {
        // Given - Initialize first time
        BalanceInitializeRequestDto request = new BalanceInitializeRequestDto();
        request.setAmount(new BigDecimal("10000000"));
        mockMvc.perform(post("/api/v1/balance/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // When & Then - Try to initialize again
        mockMvc.perform(post("/api/v1/balance/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Should return 400 Bad Request
    }

    @Test
    @WithMockUser
    void testGetCurrentBalance_Success() throws Exception {
        // Given - No balance initialized (should return 0)

        // When & Then
        mockMvc.perform(get("/api/v1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(0))
                .andExpect(jsonPath("$.initialBalance").value(0))
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.totalWithdrawal").value(0));
    }

    @Test
    @WithMockUser
    void testCheckInitialized_False() throws Exception {
        // Given - No balance initialized

        // When & Then
        mockMvc.perform(get("/api/v1/balance/check-initialized"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @WithMockUser
    void testCheckInitialized_True() throws Exception {
        // Given - Initialize balance
        BalanceInitializeRequestDto request = new BalanceInitializeRequestDto();
        request.setAmount(new BigDecimal("10000000"));
        mockMvc.perform(post("/api/v1/balance/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // When & Then
        mockMvc.perform(get("/api/v1/balance/check-initialized"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}

