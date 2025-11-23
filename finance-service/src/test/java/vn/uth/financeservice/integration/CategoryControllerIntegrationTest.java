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
import vn.uth.financeservice.dto.CategoryRequestDto;
import vn.uth.financeservice.entity.Category;
import vn.uth.financeservice.repository.CategoryRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

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
    void testCreateCategory_DuplicateName_ReturnsError() throws Exception {
        // Given
        CategoryRequestDto request = new CategoryRequestDto();
        request.setName("Entertainment");
        
        // Create first category
        mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // When & Then - Try to create duplicate
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testCreateCategory_EmptyName_ReturnsError() throws Exception {
        // Given
        CategoryRequestDto request = new CategoryRequestDto();
        request.setName(""); // Empty name

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testDeleteCategory_NotFound_ReturnsError() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/v1/categories/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void testDeleteCategory_DefaultCategory_ReturnsError() throws Exception {
        // Given - Create default category
        Category defaultCategory = new Category();
        defaultCategory.setCategoryId(UUID.randomUUID());
        defaultCategory.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        defaultCategory.setName("Default Category");
        defaultCategory.setIsDefault(true);
        defaultCategory.setCreatedAt(LocalDateTime.now());
        defaultCategory = categoryRepository.save(defaultCategory);

        // When & Then - Cannot delete default category should return 400
        final UUID defaultCategoryId = defaultCategory.getCategoryId();
        mockMvc.perform(delete("/api/v1/categories/{id}", defaultCategoryId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testGetUserCategories_ReturnsUserAndDefaultCategories() throws Exception {
        // Given - Create user category
        CategoryRequestDto request = new CategoryRequestDto();
        request.setName("My Category");
        mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name == 'My Category')]").exists());
    }
}

