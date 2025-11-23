package vn.uth.financeservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.GoalRequestDto;
import vn.uth.financeservice.dto.GoalStatusUpDate;
import vn.uth.financeservice.dto.GoalWithdrawRequestDto;
import vn.uth.financeservice.entity.*;
import vn.uth.financeservice.repository.CategoryRepository;
import vn.uth.financeservice.repository.GoalRepository;
import vn.uth.financeservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GoalServiceTest {

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private UUID testUserId;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Create test category
        testCategory = new Category();
        testCategory.setCategoryId(UUID.randomUUID());
        testCategory.setUserId(testUserId);
        testCategory.setName("Test Category");
        testCategory.setIsDefault(false);
        testCategory.setCreatedAt(LocalDateTime.now());
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    void testCreateGoal_Success() {
        // Given
        GoalRequestDto request = new GoalRequestDto();
        request.setTitle("Mua laptop");
        request.setAmount(new BigDecimal("20000000"));
        request.setEndAt(LocalDateTime.now().plusMonths(6));

        // When
        Goal goal = goalService.createGoal(testUserId, request);

        // Then
        assertNotNull(goal);
        assertNotNull(goal.getGoalId());
        assertEquals(testUserId, goal.getUserId());
        assertEquals("Mua laptop", goal.getTitle());
        assertEquals(new BigDecimal("20000000"), goal.getAmount());
        assertEquals(GoalStatus.ACTIVE, goal.getStatus());
        assertEquals(BigDecimal.ZERO, goal.getSavedAmount());
    }

    @Test
    void testWithdrawFromGoal_Success() {
        // Given
        Goal goal = createGoalWithSavedAmount(new BigDecimal("15000000"), new BigDecimal("20000000"));
        GoalWithdrawRequestDto request = new GoalWithdrawRequestDto();
        request.setAmount(new BigDecimal("5000000"));
        request.setNote("Cần gấp");

        // When
        Transaction transaction = goalService.withdrawFromGoal(goal.getGoalId(), request, testUserId);

        // Then
        assertNotNull(transaction);
        assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
        assertEquals(new BigDecimal("5000000"), transaction.getAmount());
        assertNotNull(transaction.getGoal());
        assertEquals(goal.getGoalId(), transaction.getGoal().getGoalId());
        assertTrue(transaction.getName().contains("Rút từ mục tiêu"));

        // Verify goal savedAmount decreased
        Goal updatedGoal = goalRepository.findById(goal.getGoalId()).orElseThrow();
        assertEquals(new BigDecimal("10000000"), updatedGoal.getSavedAmount());
    }

    @Test
    void testWithdrawFromGoal_InsufficientFunds_ThrowsException() {
        // Given
        Goal goal = createGoalWithSavedAmount(new BigDecimal("3000000"), new BigDecimal("20000000"));
        GoalWithdrawRequestDto request = new GoalWithdrawRequestDto();
        request.setAmount(new BigDecimal("5000000")); // More than savedAmount

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            goalService.withdrawFromGoal(goal.getGoalId(), request, testUserId);
        });
    }

    @Test
    void testWithdrawFromGoal_UnauthorizedUser_ThrowsException() {
        // Given
        Goal goal = createGoalWithSavedAmount(new BigDecimal("15000000"), new BigDecimal("20000000"));
        UUID otherUserId = UUID.randomUUID();
        GoalWithdrawRequestDto request = new GoalWithdrawRequestDto();
        request.setAmount(new BigDecimal("5000000"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            goalService.withdrawFromGoal(goal.getGoalId(), request, otherUserId);
        });
    }

    @Test
    void testWithdrawFromGoal_GoalStatusUpdated() {
        // Given - Goal is COMPLETED
        Goal goal = createGoalWithSavedAmount(new BigDecimal("20000000"), new BigDecimal("20000000"));
        goal.setStatus(GoalStatus.COMPLETED);
        goalRepository.save(goal);

        GoalWithdrawRequestDto request = new GoalWithdrawRequestDto();
        request.setAmount(new BigDecimal("5000000"));

        // When
        goalService.withdrawFromGoal(goal.getGoalId(), request, testUserId);

        // Then - Goal should be ACTIVE again (savedAmount < amount)
        Goal updatedGoal = goalRepository.findById(goal.getGoalId()).orElseThrow();
        assertEquals(GoalStatus.ACTIVE, updatedGoal.getStatus());
        assertEquals(new BigDecimal("15000000"), updatedGoal.getSavedAmount());
    }

    @Test
    void testCheckAndUpdateGoalStatus_Completed() {
        // Given
        Goal goal = createGoalWithSavedAmount(new BigDecimal("20000000"), new BigDecimal("20000000"));

        // When
        Goal updatedGoal = goalService.checkAndUpdateGoalStatus(goal);

        // Then
        assertEquals(GoalStatus.COMPLETED, updatedGoal.getStatus());
    }

    @Test
    void testCheckAndUpdateGoalStatus_Failed() {
        // Given - Goal expired with insufficient savedAmount
        Goal goal = createGoalWithSavedAmount(new BigDecimal("5000000"), new BigDecimal("20000000"));
        goal.setEndAt(LocalDateTime.now().minusDays(1)); // Expired

        // When
        Goal updatedGoal = goalService.checkAndUpdateGoalStatus(goal);

        // Then
        assertEquals(GoalStatus.FAILED, updatedGoal.getStatus());
    }

    @Test
    void testCheckAndUpdateGoalStatus_Active() {
        // Given - Goal not expired and not completed
        Goal goal = createGoalWithSavedAmount(new BigDecimal("10000000"), new BigDecimal("20000000"));
        goal.setEndAt(LocalDateTime.now().plusMonths(1)); // Not expired

        // When
        Goal updatedGoal = goalService.checkAndUpdateGoalStatus(goal);

        // Then
        assertEquals(GoalStatus.ACTIVE, updatedGoal.getStatus());
    }

    @Test
    void testWithdrawFromGoal_FromFailedGoal_StillAllows() {
        // Given - Goal is FAILED but still has savedAmount
        Goal goal = createGoalWithSavedAmount(new BigDecimal("5000000"), new BigDecimal("20000000"));
        goal.setStatus(GoalStatus.FAILED);
        goal.setEndAt(LocalDateTime.now().minusDays(1)); // Expired
        goalRepository.save(goal);

        GoalWithdrawRequestDto request = new GoalWithdrawRequestDto();
        request.setAmount(new BigDecimal("3000000"));

        // When - Should still allow withdrawal from FAILED goal
        // (Business logic: user can still withdraw even if goal failed)
        Transaction transaction = goalService.withdrawFromGoal(goal.getGoalId(), request, testUserId);
        
        // Then
        assertNotNull(transaction);
        assertEquals(TransactionType.WITHDRAWAL, transaction.getType());
        Goal updatedGoal = goalRepository.findById(goal.getGoalId()).orElseThrow();
        assertEquals(new BigDecimal("2000000"), updatedGoal.getSavedAmount());
    }

    @Test
    void testCreateGoal_WithEndAtInPast_StillCreates() {
        // Given
        GoalRequestDto request = new GoalRequestDto();
        request.setTitle("Past Goal");
        request.setAmount(new BigDecimal("10000000"));
        request.setEndAt(LocalDateTime.now().minusDays(1)); // Past date

        // When
        Goal goal = goalService.createGoal(testUserId, request);

        // Then
        assertNotNull(goal);
        assertEquals(GoalStatus.ACTIVE, goal.getStatus());
        // Status will be updated to FAILED when checked
    }

    @Test
    void testUpdateStatus_InvalidStatus_ThrowsException() {
        // Given
        Goal goal = createGoalWithSavedAmount(new BigDecimal("10000000"), new BigDecimal("20000000"));
        GoalStatusUpDate dto = new GoalStatusUpDate();
        dto.setStatus("INVALID_STATUS");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            goalService.updateStatus(goal.getGoalId(), dto, testUserId);
        });
    }

    private Goal createGoalWithSavedAmount(BigDecimal savedAmount, BigDecimal targetAmount) {
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

