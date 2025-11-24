package vn.uth.financeservice.dto;

public class GoalSummaryDto {
    private String title;
    private Double prog; // progress percentage
    private Long days; // days remaining
    private Boolean risk; // at risk

    public GoalSummaryDto() {}

    public GoalSummaryDto(String title, Double prog, Long days, Boolean risk) {
        this.title = title;
        this.prog = prog;
        this.days = days;
        this.risk = risk;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getProg() {
        return prog;
    }

    public void setProg(Double prog) {
        this.prog = prog;
    }

    public Long getDays() {
        return days;
    }

    public void setDays(Long days) {
        this.days = days;
    }

    public Boolean getRisk() {
        return risk;
    }

    public void setRisk(Boolean risk) {
        this.risk = risk;
    }
}

