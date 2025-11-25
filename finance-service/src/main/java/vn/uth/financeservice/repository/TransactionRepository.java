package vn.uth.financeservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.uth.financeservice.entity.Transaction;
import vn.uth.financeservice.entity.TransactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserId(UUID userId);
    
    List<Transaction> findByUserIdAndTypeAndStatus(UUID userId, TransactionType type, String status);
    
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.type = :type AND t.status = :status AND t.goal IS NULL")
    List<Transaction> findByUserIdAndTypeAndStatusAndGoalIsNull(UUID userId, TransactionType type, String status);
    
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.type = :type AND t.status = :status AND t.goal IS NOT NULL")
    List<Transaction> findByUserIdAndTypeAndStatusAndGoalIsNotNull(UUID userId, TransactionType type, String status);
    
    List<Transaction> findByUserIdAndTypeAndStatusAndTransactionDateBetween(
            UUID userId, TransactionType type, String status, 
            LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.status = :status ORDER BY t.transactionDate DESC, t.createdAt DESC")
    Page<Transaction> findByUserIdAndStatusOrderByTransactionDateDesc(
            @Param("userId") UUID userId, 
            @Param("status") String status, 
            Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.status = :status AND t.transactionDate >= :startDate AND t.transactionDate < :endDate ORDER BY t.transactionDate DESC, t.createdAt DESC")
    Page<Transaction> findByUserIdAndStatusAndTransactionDateBetweenOrderByTransactionDateDesc(
            @Param("userId") UUID userId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.status = :status ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<Transaction> findTopByUserIdAndStatusOrderByTransactionDateDesc(
            @Param("userId") UUID userId, 
            @Param("status") String status, 
            Pageable pageable);
    
    default List<Transaction> findTopByUserIdAndStatusOrderByTransactionDateDesc(UUID userId, String status, int limit) {
        return findTopByUserIdAndStatusOrderByTransactionDateDesc(userId, status, org.springframework.data.domain.Pageable.ofSize(limit));
    }
    
    @Query("SELECT t FROM Transaction t WHERE t.goal.goalId = :goalId")
    List<Transaction> findByGoalId(@Param("goalId") UUID goalId);
    
    @Query("SELECT t FROM Transaction t WHERE t.category.categoryId = :categoryId")
    List<Transaction> findByCategoryId(@Param("categoryId") UUID categoryId);
    
    /**
     * Lấy tất cả transactions của một category trong khoảng thời gian cụ thể
     * Dùng cho Category Transactions API
     */
    @Query("SELECT t FROM Transaction t WHERE t.category.categoryId = :categoryId AND t.status = :status AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findByCategoryIdAndStatusAndTransactionDateBetween(
            @Param("categoryId") UUID categoryId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}


