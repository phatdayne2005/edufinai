// src/main/java/vn/uth/authservice/service/impl/AuthServiceImpl.java
package vn.uth.authservice.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.uth.authservice.dto.AuthResponse;
import vn.uth.authservice.dto.LoginRequest;
import vn.uth.authservice.dto.RegisterRequest;
import vn.uth.authservice.entity.User;
import vn.uth.authservice.enums.Role;
import vn.uth.authservice.repository.UserRepository;
import vn.uth.authservice.security.JwtService;
import vn.uth.authservice.service.AuthService;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepo;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public AuthServiceImpl(UserRepository userRepo, PasswordEncoder encoder, JwtService jwt) {
    this.userRepo = userRepo;
    this.encoder = encoder;
    this.jwt = jwt;
  }

  @Override
  public AuthResponse register(RegisterRequest req) {
    if (userRepo.existsByUsername(req.username())) throw new RuntimeException("Username taken");
    if (userRepo.existsByEmail(req.email())) throw new RuntimeException("Email taken");

    User u = new User();
    u.setUserId(UUID.randomUUID());
    u.setUsername(req.username());
    u.setPasswordHash(encoder.encode(req.password()));
    u.setEmail(req.email());
    u.setRole(Role.LEARNER);
    userRepo.save(u);

    String token = jwt.generateToken(
        u.getUsername(),
        Map.of("role", u.getRole().name(), "uid", u.getUserId().toString())
    );
    return new AuthResponse(token, u.getRole().name());
  }

  @Override
  public AuthResponse login(LoginRequest req) {
    User u = userRepo.findByUsername(req.username())
        .orElseThrow(() -> new RuntimeException("Bad credentials"));
    if (!encoder.matches(req.password(), u.getPasswordHash())) {
      throw new RuntimeException("Bad credentials");
    }
    String token = jwt.generateToken(
        u.getUsername(),
        Map.of("role", u.getRole().name(), "uid", u.getUserId().toString())
    );
    return new AuthResponse(token, u.getRole().name());
  }
}
