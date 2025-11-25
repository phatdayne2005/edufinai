package vn.uth.gamificationservice.dto;

public class SimpleResponse {
    private String status;

    public SimpleResponse() {
    }

    public SimpleResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
