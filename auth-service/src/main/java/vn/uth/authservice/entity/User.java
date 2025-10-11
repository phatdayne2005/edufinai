package vn.uth.authservice.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import vn.uth.authservice.enums.Role; import vn.uth.authservice.enums.Status;
import java.time.LocalDateTime; import java.util.UUID;

@Entity @Table(name="users")
@Getter @Setter
public class User {
  @Id
  @Column(name="user_id", columnDefinition="BINARY(16)")
  private UUID userId;

  @Column(nullable=false, unique=true, length=50)
  private String username;

  @Column(name="password_hash", nullable=false, length=100)
  private String passwordHash;

  @Column(nullable=false, unique=true, length=120)
  private String email;

  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false)
  private Role role = Role.LEARNER;

  private String avatarUrl;

  @Column(columnDefinition="json")
  private String financeProfile;

  @Column(columnDefinition="json")
  private String goals;

  private LocalDateTime lastLogin;

  @Enumerated(EnumType.STRING)
  @Column(nullable=false)
  private Status status = Status.ACTIVE;

  @Column(nullable=false, updatable=false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(nullable=false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @PreUpdate void onUpdate(){ this.updatedAt = LocalDateTime.now(); }
}
