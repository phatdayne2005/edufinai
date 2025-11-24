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
import vn.uth.financeservice.dto.TransactionRequestDto;
import vn.uth.financeservice.entity.Category;
import vn.uth.financeservice.entity.TransactionType;
import vn.uth.financeservice.repository.CategoryRepository;
import vn.uth.financeservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockitoBean
    private AuthServiceClient authServiceClient;

    private UUID testUserId;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        when(authServiceClient.getCurrentUserId()).thenReturn(testUserId);

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
    @WithMockUser
    void testCreateTransaction_InvalidCategory_ReturnsError() throws Exception {
        // Given
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("INCOME");
        request.setAmount(new BigDecimal("5000000"));
        request.setName("Test");
        request.setCategoryId(UUID.randomUUID()); // Non-existent category

        // When & Then - Category not found should return 404
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void testCreateTransaction_MissingRequiredFields_ReturnsError() throws Exception {
        // Given - Missing name
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("INCOME");
        request.setAmount(new BigDecimal("5000000"));
        request.setCategoryId(testCategory.getCategoryId());
        // name is missing

        // When & Then
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testCreateTransaction_InvalidType_ReturnsError() throws Exception {
        // Given
        TransactionRequestDto request = new TransactionRequestDto();
        request.setType("INVALID_TYPE");
        request.setAmount(new BigDecimal("5000000"));
        request.setName("Test");
        request.setCategoryId(testCategory.getCategoryId());

        // When & Then - Invalid type should return 400 (IllegalArgumentException)
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testDeleteTransaction_NotFound_ReturnsError() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then - Transaction not found should return 404
        mockMvc.perform(delete("/api/v1/transactions/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void testGetTransactions_WithPagination() throws Exception {
        // Given - Create some transactions
        for (int i = 0; i < 5; i++) {
            final int transactionNumber = i; // Make effectively final
            TransactionRequestDto request = new TransactionRequestDto();
            request.setType("INCOME");
            request.setAmount(new BigDecimal("1000000"));
            request.setName("Transaction " + transactionNumber);
            request.setCategoryId(testCategory.getCategoryId());
            mockMvc.perform(post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // When & Then
        mockMvc.perform(get("/api/v1/transactions")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}

