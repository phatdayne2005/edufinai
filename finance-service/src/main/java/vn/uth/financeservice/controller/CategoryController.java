package vn.uth.financeservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<List<Category>> getUserCategories(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(categoryService.getUserCategories(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Validated CategoryRequestDto dto,
                                    Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(categoryService.createCategory(userId, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        categoryService.deleteCategory(id, userId);
        return ResponseEntity.ok().build();
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Unauthenticated request");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID uuid) {
            return uuid;
        }
        if (principal instanceof String value) {
            return UUID.fromString(value);
        }
        throw new RuntimeException("Invalid authentication principal");
    }
}

