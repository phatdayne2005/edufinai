package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.*;
import java.util.*;

@Data
//@NoArgsConstructor
//@AllArgsConstructor
public class Lesson {

    @Id
    @Column(name = "lesson_id")
    @GeneratedValue
    private UUID id;

    @ManyToMany(mappedBy = "lessons")
    private List<Learner> learners;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private Creator creator;

    @ManyToOne()
    @JoinColumn(name = "moderator_id")
    private Moderator moderator;

    @NotBlank
    @Size(max = 150)
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @NotBlank
    @Size(max = 180)
    @Column(name = "slug", nullable = false, length = 180, unique = true)
    private String slug;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    // Rich text / Markdown / HTML
    @NotBlank
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", nullable = false)
    private String content;

    // minutes
    @NotBlank
    @Min(1)
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 5;

    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 20, nullable = false)
    private Difficulty difficulty = Difficulty.BASIC;

    // DRAFT/PENDING/APPROVED/REJECTED
    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status = Status.DRAFT;

    // Optional media
    @Size(max = 255)
    @Column(name = "thumbnail_url", length = 255)
    private String thumbnailUrl;

    @Size(max = 255)
    @Column(name = "video_url", length = 255)
    private String videoUrl;

    // Simple tagging without extra file (ElementCollection → join table)
    @ElementCollection
    @CollectionTable(name = "lesson_tags", joinColumns = @JoinColumn(name = "lesson_id"))
    @Column(name = "tag", length = 50)
    private Set<String> tags = new HashSet<>();

    // Quiz payload (JSON-as-text); keep flexible for now
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "quiz_json")
    private String quizJson;

    @Column(name = "quiz_result")
    private String quizResult;

    @Column(name = "rating")
    private float rating;

    // Versioning & auditing
    @Version
    private Long version;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @NotBlank
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotBlank
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Difficulty { BASIC, INTERMEDIATE, ADVANCED }
    public enum Status { DRAFT, PENDING, APPROVED, REJECTED }

    /*private String slugify(String input) {
        String s = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9\\s-]", "");
        s = s.replaceAll("\\s+", "-");
        return s.length() > 180 ? s.substring(0, 180) : s;
    }*/
}
