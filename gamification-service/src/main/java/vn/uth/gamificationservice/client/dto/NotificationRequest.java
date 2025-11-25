package vn.uth.gamificationservice.client.dto;

import java.util.HashMap;
import java.util.Map;

public class NotificationRequest {
    private String title;
    private String body;
    private Map<String, Object> data;

    public NotificationRequest() {
        this.data = new HashMap<>();
    }

    public NotificationRequest(String title, String body, Map<String, Object> data) {
        this.title = title;
        this.body = body;
        this.data = data != null ? data : new HashMap<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data != null ? data : new HashMap<>();
    }
}

