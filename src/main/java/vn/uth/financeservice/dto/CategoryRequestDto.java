package vn.uth.financeservice.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoryRequestDto {
    @NotBlank
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}


