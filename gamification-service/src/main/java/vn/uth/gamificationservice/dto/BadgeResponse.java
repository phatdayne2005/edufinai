package vn.uth.gamificationservice.dto;

import vn.uth.gamificationservice.model.BadgeType;

import java.util.UUID;

public class BadgeResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private BadgeType type;
    private String iconUrl;

    public BadgeResponse() {
    }

    public BadgeResponse(UUID id, String code, String name, String description, BadgeType type, String iconUrl) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.type = type;
        this.iconUrl = iconUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

    public BadgeType getType() {
        return type;
    }

    public void setType(BadgeType type) {
        this.type = type;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}

