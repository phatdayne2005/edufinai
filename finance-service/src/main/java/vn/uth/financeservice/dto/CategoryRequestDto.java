package vn.uth.financeservice.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoryRequestDto {
    @NotBlank
    private String name;
    
    private String type; // INCOME, EXPENSE, BOTH (optional, default: EXPENSE)

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}


