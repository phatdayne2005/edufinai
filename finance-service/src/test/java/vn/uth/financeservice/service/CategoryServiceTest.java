package vn.uth.financeservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.CategoryRequestDto;
import vn.uth.financeservice.entity.Category;
import vn.uth.financeservice.repository.CategoryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    private UUID testUserId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
    }

    @Test
    void testCreateCategory_Success() {
        // Given
        CategoryRequestDto request = new CategoryRequestDto();
        request.setName("Entertainment");

        // When
        Category category = categoryService.createCategory(testUserId, request);

        // Then
        assertNotNull(category);
        assertNotNull(category.getCategoryId());
        assertEquals(testUserId, category.getUserId());
        assertEquals("Entertainment", category.getName());
        assertFalse(category.getIsDefault());
    }

    @Test
    void testCreateCategory_DuplicateName_ThrowsException() {
        // Given
        CategoryRequestDto request = new CategoryRequestDto();
        request.setName("Entertainment");
        categoryService.createCategory(testUserId, request);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            categoryService.createCategory(testUserId, request);
        });
    }

    @Test
    void testCreateCategory_SameNameDifferentUsers_Success() {
        // Given
        CategoryRequestDto request = new CategoryRequestDto();
        request.setName("Entertainment");
        categoryService.createCategory(testUserId, request);

        // When - Different user can create same name
        Category category2 = categoryService.createCategory(otherUserId, request);

        // Then
        assertNotNull(category2);
        assertEquals(otherUserId, category2.getUserId());
        assertEquals("Entertainment", category2.getName());
    }

    @Test
    void testDeleteCategory_Success() {
        // Given
        Category category = createCategory(testUserId, "Test Category");

        // When
        categoryService.deleteCategory(category.getCategoryId(), testUserId);

        // Then
        assertFalse(categoryRepository.findById(category.getCategoryId()).isPresent());
    }

    @Test
    void testDeleteCategory_NotFound_ThrowsException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            categoryService.deleteCategory(nonExistentId, testUserId);
        });
    }

    @Test
    void testDeleteCategory_OtherUserCategory_ThrowsException() {
        // Given
        Category category = createCategory(testUserId, "Test Category");

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            categoryService.deleteCategory(category.getCategoryId(), otherUserId);
        });
    }

    @Test
    void testDeleteCategory_DefaultCategory_ThrowsException() {
        // Given - Create a default category
        Category defaultCategory = new Category();
        defaultCategory.setCategoryId(UUID.randomUUID());
        defaultCategory.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        defaultCategory.setName("Default Category");
        defaultCategory.setIsDefault(true);
        defaultCategory.setCreatedAt(LocalDateTime.now());
        defaultCategory = categoryRepository.save(defaultCategory);
        
        final UUID defaultCategoryId = defaultCategory.getCategoryId();
        final UUID userId = testUserId;

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            categoryService.deleteCategory(defaultCategoryId, userId);
        });
    }

    @Test
    void testGetUserCategories_IncludesDefaultCategories() {
        // Given - Create default category
        final String defaultCategoryName = "Ăn uống";
        final String userCategoryName = "My Category";
        
        Category defaultCategory = new Category();
        defaultCategory.setCategoryId(UUID.randomUUID());
        defaultCategory.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        defaultCategory.setName(defaultCategoryName);
        defaultCategory.setIsDefault(true);
        defaultCategory.setCreatedAt(LocalDateTime.now());
        categoryRepository.save(defaultCategory);

        // Create user category
        Category userCategory = createCategory(testUserId, userCategoryName);

        // When
        List<Category> categories = categoryService.getUserCategories(testUserId);

        // Then
        assertTrue(categories.size() >= 2);
        assertTrue(categories.stream().anyMatch(c -> c.getName().equals(userCategoryName)));
        assertTrue(categories.stream().anyMatch(c -> c.getName().equals(defaultCategoryName)));
    }

    private Category createCategory(UUID userId, String name) {
        Category category = new Category();
        category.setCategoryId(UUID.randomUUID());
        category.setUserId(userId);
        category.setName(name);
        category.setIsDefault(false);
        category.setCreatedAt(LocalDateTime.now());
        return categoryRepository.save(category);
    }
}

