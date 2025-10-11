package vn.uth.authservice.controller;

import lombok.RequiredArgsConstructor; import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated; import org.springframework.web.bind.annotation.*;
import vn.uth.authservice.dto.*; import vn.uth.authservice.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor @Validated
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody @Validated RegisterRequest req){
    return ResponseEntity.ok(authService.register(req));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody @Validated LoginRequest req){
    return ResponseEntity.ok(authService.login(req));
  }
}
