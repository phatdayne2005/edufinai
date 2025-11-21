package vn.uth.financeservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.financeservice.dto.CategoryRequestDto;
import vn.uth.financeservice.entity.Category;
import vn.uth.financeservice.repository.CategoryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> getUserCategories(UUID userId) {
        // Lấy cả categories của user và default categories
        return categoryRepository.findByUserIdOrIsDefaultTrue(userId);
    }

    @Transactional
    public Category createCategory(UUID userId, CategoryRequestDto request) {
        // Kiểm tra xem category đã tồn tại chưa
        if (categoryRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new RuntimeException("Category already exists");
        }
        
        Category category = new Category();
        category.setCategoryId(UUID.randomUUID());
        category.setUserId(userId);
        category.setName(request.getName());
        category.setIsDefault(false);
        category.setCreatedAt(LocalDateTime.now());
        
        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(UUID categoryId, UUID userId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        // Không cho phép xóa default categories
        if (category.getIsDefault()) {
            throw new RuntimeException("Cannot delete default category");
        }
        
        // Chỉ cho phép xóa category của chính user đó
        if (!category.getUserId().equals(userId)) {
            throw new RuntimeException("Cannot delete other user's category");
        }
        
        categoryRepository.delete(category);
    }
}

