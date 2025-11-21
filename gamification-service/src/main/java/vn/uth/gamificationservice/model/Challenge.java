package vn.uth.gamificationservice.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "challenges")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Challenge {
    @Id
    @GeneratedValue
    @Column(name = "challenge_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "title")
    @NotNull
    private String title;

    @Column(name = "description")
    @NotNull
    private String description;

    @Column(name = "type")
    @NotNull
    @Enumerated(EnumType.STRING)
    private ChallengeType type;

    @Column(name = "scope")
    @NotNull
    @Enumerated(EnumType.STRING)
    private ChallengeScope scope;

    @Column(name = "target_value")
    private Integer targetValue;

    @Column(name = "start_at")
    @NotNull
    private ZonedDateTime startAt;

    @Column(name = "end_at")
    @NotNull
    private ZonedDateTime endAt;

    @Column(name = "active")
    @NotNull
    private boolean active;

    @Column(name = "rule")
    @NotNull
    private String rule;

    @Column(name = "reward_score")
    private Integer rewardScore;

    @Column(name = "reward_badge_code")
    private String rewardBadgeCode;

    @Column(name = "max_progress_per_day")
    private Integer maxProgressPerDay;

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = ZonedDateTime.now(ZoneId.systemDefault());
        }
    }

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
