package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.uth.financeservice.dto.CategoryRequestDto;
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
}

