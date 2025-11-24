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
import vn.uth.financeservice.dto.GoalRequestDto;
import vn.uth.financeservice.dto.GoalWithdrawRequestDto;
import vn.uth.financeservice.entity.Goal;
import vn.uth.financeservice.entity.GoalStatus;
import vn.uth.financeservice.repository.GoalRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GoalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GoalRepository goalRepository;

    @MockitoBean
    private AuthServiceClient authServiceClient;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        when(authServiceClient.getCurrentUserId()).thenReturn(testUserId);
    }

    @Test
    @WithMockUser
    void testWithdrawFromGoal_Success() throws Exception {
        // Given
        Goal goal = createGoal(new BigDecimal("15000000"), new BigDecimal("20000000"));
        GoalWithdrawRequestDto request = new GoalWithdrawRequestDto();
        request.setAmount(new BigDecimal("5000000"));
        request.setNote("Cần gấp");

        // When & Then
        mockMvc.perform(post("/api/v1/goals/{id}/withdraw", goal.getGoalId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(5000000))
                .andExpect(jsonPath("$.goalId").value(goal.getGoalId().toString()));

        // Verify goal savedAmount decreased
        Goal updatedGoal = goalRepository.findById(goal.getGoalId()).orElseThrow();
        assertEquals(new BigDecimal("10000000"), updatedGoal.getSavedAmount());
    }

    @Test
    @WithMockUser
    void testWithdrawFromGoal_InsufficientFunds_ReturnsError() throws Exception {
        // Given
        Goal goal = createGoal(new BigDecimal("3000000"), new BigDecimal("20000000"));
        GoalWithdrawRequestDto request = new GoalWithdrawRequestDto();
        request.setAmount(new BigDecimal("5000000")); // More than savedAmount

        // When & Then
        mockMvc.perform(post("/api/v1/goals/{id}/withdraw", goal.getGoalId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Should return 400 Bad Request
    }

    @Test
    @WithMockUser
    void testWithdrawFromGoal_GoalNotFound_ReturnsError() throws Exception {
        // Given
        UUID nonExistentGoalId = UUID.randomUUID();
        GoalWithdrawRequestDto request = new GoalWithdrawRequestDto();
        request.setAmount(new BigDecimal("5000000"));

        // When & Then
        mockMvc.perform(post("/api/v1/goals/{id}/withdraw", nonExistentGoalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()); // Should return 404 Not Found
    }

    private Goal createGoal(BigDecimal savedAmount, BigDecimal targetAmount) {
        Goal goal = new Goal();
        goal.setGoalId(UUID.randomUUID());
        goal.setUserId(testUserId);
        goal.setTitle("Test Goal");
        goal.setAmount(targetAmount);
        goal.setSavedAmount(savedAmount);
        goal.setStartAt(LocalDateTime.now());
        goal.setEndAt(LocalDateTime.now().plusMonths(6));
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setNewStatus(GoalStatus.ACTIVE);
        goal.setUpdatedAt(LocalDateTime.now());
        return goalRepository.save(goal);
    }
}

