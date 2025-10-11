package vn.uth.authservice.service.impl;

import lombok.RequiredArgsConstructor; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service;
import vn.uth.authservice.dto.*; import vn.uth.authservice.entity.User;
import vn.uth.authservice.enums.Role; import vn.uth.authservice.repository.UserRepository;
import vn.uth.authservice.security.JwtService;
import java.util.Map; import java.util.UUID;

@Service @RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final UserRepository userRepo;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

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

    String token = jwt.generateToken(u.getUsername(), Map.of("role", u.getRole().name(), "uid", u.getUserId().toString()));
    return new AuthResponse(token, u.getRole().name());
  }

  @Override
  public AuthResponse login(LoginRequest req) {
    var u = userRepo.findByUsername(req.username()).orElseThrow(() -> new RuntimeException("Bad credentials"));
    if (!encoder.matches(req.password(), u.getPasswordHash())) throw new RuntimeException("Bad credentials");
    String token = jwt.generateToken(u.getUsername(), Map.of("role", u.getRole().name(), "uid", u.getUserId().toString()));
    return new AuthResponse(token, u.getRole().name());
  }
}
