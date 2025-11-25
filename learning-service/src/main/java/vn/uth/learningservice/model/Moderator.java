package vn.uth.learningservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "moderator")
@Getter
@Setter
@NoArgsConstructor
public class Moderator {

    @Id
    @Column(name = "moderator_id", nullable = false)
    @NotNull
    private UUID id;

    @OneToMany(mappedBy = "moderator")
    private List<Lesson> lessons = new ArrayList<>();
}
