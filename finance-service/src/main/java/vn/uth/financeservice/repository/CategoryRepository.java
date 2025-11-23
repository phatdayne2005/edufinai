package vn.uth.financeservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.financeservice.entity.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserId(UUID userId);
    List<Category> findByUserIdOrIsDefaultTrue(UUID userId);
    Optional<Category> findByUserIdAndName(UUID userId, String name);
    boolean existsByUserIdAndName(UUID userId, String name);
    
    // Tìm category "Khác" (default category, type = BOTH)
    Optional<Category> findByNameAndIsDefaultTrue(String name);
}


