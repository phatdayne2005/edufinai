package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;
import java.util.*;

@Data
public class Learner {

    @Id
    @Column(name = "learner_id")
    @GeneratedValue(strategy =  GenerationType.UUID)
    private UUID id;

    @OneToMany(mappedBy = "learner")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<Enrollment> enrollments = new ArrayList<>();

    @Size(max = 50)
    @Column(name = "display_name", length = 50)
    private String displayName;

    // Dob (ngày sinh)
    @Past(message = "dob phải nhỏ hơn ngày hiện tại")
    @Column(name = "dob")
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", length = 20, nullable = false)
    private Level level = Level.BEGINNER;

    @Size(max = 255)
    @Column(name = "avatar", length = 255)
    private String avatar;

    @Size(max = 500)
    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "total_points")
    private Integer totalPoints = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Level { BEGINNER, INTERMEDIATE, ADVANCED }
}
