package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

public class ContentModerator {
    @Id
    @Column(name = "moderator_id", columnDefinition = "CHAR(36)")
    private UUID id;

    // Cross-service reference to User Service
    @NotNull
    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false, unique = true)
    private UUID userId;

    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    // Simple role-scope text, e.g. "FIN_EDU_ONLY;QUIZ_ONLY"
    @Size(max = 255)
    @Column(name = "scope", length = 255)
    private String scope;

    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "handled_items", nullable = false)
    private Integer handledItems = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
