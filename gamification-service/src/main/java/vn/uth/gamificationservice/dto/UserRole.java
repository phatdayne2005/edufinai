package vn.uth.gamificationservice.dto;

import java.util.List;

public class UserRole {
    private String name;
    private String description;
    private List<String> permissions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", permissions=" + permissions +
                '}';
    }
}
