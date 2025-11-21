package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "creator")
@Getter
@Setter
@NoArgsConstructor
public class Creator {

    @Id
    @Column(name = "creator_id", nullable = false)
    @NotNull
    private UUID id;

    // Cross-service reference to Lesson
    @OneToMany(mappedBy = "creator")
    private List<Lesson> lessons = new ArrayList<>();
}
