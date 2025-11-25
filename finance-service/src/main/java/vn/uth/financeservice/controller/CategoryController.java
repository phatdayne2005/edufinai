package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.uth.financeservice.dto.CategoryRequestDto;
import vn.uth.financeservice.dto.CategoryTransactionsDto;
import vn.uth.financeservice.entity.Category;
import vn.uth.financeservice.service.CategoryService;
import vn.uth.financeservice.client.AuthServiceClient;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthServiceClient authServiceClient;

    @GetMapping
    public ResponseEntity<List<Category>> getUserCategories() {
        UUID userId = authServiceClient.getCurrentUserId();
        return ResponseEntity.ok(categoryService.getUserCategories(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Validated CategoryRequestDto dto) {
        UUID userId = authServiceClient.getCurrentUserId();
        return ResponseEntity.ok(categoryService.createCategory(userId, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        UUID userId = authServiceClient.getCurrentUserId();
        categoryService.deleteCategory(id, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * API: GET /api/v1/categories/{id}/transactions
     * Lấy tất cả transactions của một category trong khoảng thời gian
     * 
     * @param id UUID của category
     * @param month Tháng (1-12), optional, default = tháng hiện tại
     * @param year Năm (2024, 2025...), optional, default = năm hiện tại
     * @param page Số trang (0-based), default = 0
     * @param size Số items/trang, default = 20
     * @return CategoryTransactionsDto
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<CategoryTransactionsDto> getCategoryTransactions(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = authServiceClient.getCurrentUserId();
        
        CategoryTransactionsDto result = categoryService.getCategoryTransactions(
                id, userId, month, year, page, size
        );
        
        return ResponseEntity.ok(result);
    }
}

