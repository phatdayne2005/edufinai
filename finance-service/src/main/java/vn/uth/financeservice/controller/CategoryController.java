package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.uth.financeservice.dto.CategoryRequestDto;
import vn.uth.financeservice.entity.Category;
import vn.uth.financeservice.service.CategoryService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getUserCategories() {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes()); // sau này sẽ thay bằng JWT
        return ResponseEntity.ok(categoryService.getUserCategories(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Validated CategoryRequestDto dto) {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes()); // sau này sẽ thay bằng JWT
        return ResponseEntity.ok(categoryService.createCategory(userId, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        UUID userId = UUID.nameUUIDFromBytes("demo-user".getBytes()); // sau này sẽ thay bằng JWT
        categoryService.deleteCategory(id, userId);
        return ResponseEntity.ok().build();
    }
}

