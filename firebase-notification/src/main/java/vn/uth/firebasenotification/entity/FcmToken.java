package vn.uth.firebasenotification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_fcm_tokens", indexes = {@Index(name="idx_user", columnList = "user_id")})
public class FcmToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Column(length = 512, unique = true)
    private String token;
    private String platform;
    @Column(columnDefinition = "json")
    private String deviceInfo;
    private Boolean isActive = true;
    private Timestamp createdAt;
    private Timestamp lastSeen;
    // getters/setters (or @Data lombok)
}

