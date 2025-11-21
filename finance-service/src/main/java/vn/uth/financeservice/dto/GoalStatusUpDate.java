package vn.uth.financeservice.dto;

import jakarta.validation.constraints.NotNull;

public class GoalStatusUpDate {
    @NotNull
    private String status; // ACTIVE, COMPLETED, FAILED

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

