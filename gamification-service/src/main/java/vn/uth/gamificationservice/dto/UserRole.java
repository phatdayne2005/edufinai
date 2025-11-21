package vn.uth.gamificationservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserRole {
    private String name;
    private String description;
    private List<String> permissions;
}
