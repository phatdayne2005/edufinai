package vn.uth.learningservice.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.learningservice.model.Lesson;

import java.util.*;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

        Optional<Lesson> findBySlug(String slug); // vì slug unique ở DB

        boolean existsBySlug(String slug); // để validate khi tạo/sửa

        // Liệt kê theo trạng thái duyệt
        List<Lesson> findByStatus(Lesson.Status status);

        // Liệt kê các bài của 1 creator
        List<Lesson> findByCreator_Id(UUID creatorId);

        // Liệt kê các bài của 1 moderator theo trạng thái (ví dụ PENDING)
        List<Lesson> findByModerator_IdAndStatus(UUID moderatorId, Lesson.Status status);

        // Lọc theo độ khó
        List<Lesson> findByDifficulty(Lesson.Difficulty difficulty);

        long countByModerator_IdAndStatus(UUID moderatorId, Lesson.Status status);

        // Dành cho trang public: đã APPROVED và đã có publishedAt
        @Query("""
                        select ls from Lesson ls
                        where ls.status = 'APPROVED'
                        """)
        List<Lesson> findPublished();

        // Tìm kiếm đơn giản theo tiêu đề/mô tả
        @Query("""
                        select ls from Lesson ls
                        where (lower(ls.title) like lower(concat('%', :q, '%'))
                            or lower(ls.description) like lower(concat('%', :q, '%')))
                        """)
        List<Lesson> search(@Param("q") String keyword);

        // (Tuỳ chọn) lọc theo tag nếu bạn map bảng lesson_tags thành
        // @ElementCollection;
        // nếu chưa có entity cho tag, có thể dùng native query:
        @Query(value = """
                        select l.* from lesson l
                        join lesson_tags t on t.lesson_id = l.lesson_id
                        where t.tag = :tag
                        """, nativeQuery = true)
        List<Lesson> findByTag(@Param("tag") String tag);

        // Duyệt bài: cập nhật trạng thái/approvedAt/publishedAt nhanh gọn
        @Modifying
        @Transactional
        @Query("""
                        update Lesson ls
                           set ls.status = :status,
                               ls.approvedAt = CASE WHEN :status = 'APPROVED' THEN CURRENT_TIMESTAMP ELSE ls.approvedAt END,
                               ls.publishedAt = CASE WHEN :publishNow = true THEN CURRENT_TIMESTAMP ELSE ls.publishedAt END
                         where ls.id = :lessonId
                        """)
        int moderateAndPublish(@Param("lessonId") UUID lessonId,
                        @Param("status") Lesson.Status status,
                        @Param("publishNow") boolean publishNow);

        // Calculate total questions for a difficulty level (for EXP calculation)
        @Query("""
                        select coalesce(sum(l.totalQuestions), 0)
                        from Lesson l
                        where l.difficulty = :difficulty
                        and l.status = 'APPROVED'
                        """)
        int getTotalQuestionsByDifficulty(@Param("difficulty") Lesson.Difficulty difficulty);
}
