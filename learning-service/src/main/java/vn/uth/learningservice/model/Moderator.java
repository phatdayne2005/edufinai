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
    @GeneratedValue(strategy =  GenerationType.UUID)
    private UUID id;

    @OneToMany(mappedBy = "moderator")
    private List<Lesson> lessons;

    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    @Size(max = 500)
    @Column(name = "comment", length = 500)
    private String comment;

    // Bỏ
//    @Column(name = "handled_items", nullable = false)
//    private Integer handledItems = 0;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
