package vn.uth.gamificationservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "challenge_approval_history")
@Getter
@Setter
public class ChallengeApprovalHistory {

    @Id
    @GeneratedValue
    @Column(name = "history_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChallengeApprovalStatus status;

    @Column(name = "reviewer_id", columnDefinition = "BINARY(16)")
    private UUID reviewerId;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;
}

