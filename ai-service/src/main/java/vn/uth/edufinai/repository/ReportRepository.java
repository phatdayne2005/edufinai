package vn.uth.edufinai.repository;

import vn.uth.edufinai.model.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    /**
     * Tìm report theo ngày (chỉ so sánh date part, bỏ qua time)
     * Sử dụng native query với DATE() function để so sánh date part của ZonedDateTime
     */
    @Query(value = "SELECT * FROM ai_reports WHERE DATE(report_date) = :reportDate", nativeQuery = true)
    Optional<ReportEntity> findByReportDate(@Param("reportDate") LocalDate reportDate);
}
