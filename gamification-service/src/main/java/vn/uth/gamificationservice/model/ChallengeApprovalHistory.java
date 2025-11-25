package vn.uth.gamificationservice.model;

import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "challenge_approval_history")
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

    public UUID getId() {
        return id;
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    public ChallengeApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ChallengeApprovalStatus status) {
        this.status = status;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

