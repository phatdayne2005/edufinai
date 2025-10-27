package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class Moderator {

    @Id
    @Column(name = "moderator_id")
    @GeneratedValue
    private UUID id;

    @OneToMany(mappedBy = "moderator")
    private List<Lesson> lessons;

    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    @Size(max = 500)
    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "handled_items", nullable = false)
    private Integer handledItems = 0;

    @NotBlank
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotBlank
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
