package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "lesson", uniqueConstraints = @UniqueConstraint(columnNames = "slug"))
@Getter
@Setter
@NoArgsConstructor
public class Lesson {
    @Id
    @Column(name = "lesson_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToMany(mappedBy = "lesson")
    private List<Enrollment> enrollments = new ArrayList<>();

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
    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    // Rich text / Markdown / HTML
    @NotBlank
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    // minutes
    @NotNull
    @Min(0)
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", length = 20, nullable = false)
    private Difficulty difficulty = Difficulty.BASIC;

    // DRAFT/PENDING/APPROVED/REJECTED
    @NotNull
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
    @CollectionTable(name = "lesson_tags", joinColumns = @JoinColumn(name = "lesson_id"), uniqueConstraints = @UniqueConstraint(columnNames = {
            "lesson_id", "tag" }))
    @Column(name = "tag", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Tag> tags = new HashSet<>();

    // Quiz payload (JSON-as-text)
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "quiz_json", columnDefinition = "LONGTEXT")
    private String quizJson;

    @Column(name = "total_questions")
    private Integer totalQuestions = 0;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "comment_by_mod", columnDefinition = "LONGTEXT")
    private String commentByMod;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Difficulty {
        BASIC, INTERMEDIATE, ADVANCED
    }

    public enum Status {
        DRAFT, PENDING, APPROVED, REJECTED
    }

    public enum Tag {
        BUDGETING, INVESTING, SAVING, DEBT, TAX
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "lesson";
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9\\s-]", "");
        s = s.replaceAll("\\s+", "-");
        s = s.replaceAll("-+", "-");
        s = s.replaceAll("^-|-$", "");
        return s.length() > 180 ? s.substring(0, 180) : (s.isEmpty() ? "lesson" : s);
    }
}
