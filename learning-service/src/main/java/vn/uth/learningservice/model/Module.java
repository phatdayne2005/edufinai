package vn.uth.learningservice.model;

import jakarta.persistence.*;
import lombok.*;

@Data
//@NoArgsConstructor
//@AllArgsConstructor
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String module_id;

    private String title;

    private String description;
}
