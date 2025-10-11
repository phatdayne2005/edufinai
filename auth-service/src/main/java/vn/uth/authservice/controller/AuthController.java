// src/main/java/vn/uth/authservice/controller/AuthController.java
package vn.uth.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.uth.authservice.dto.AuthResponse;
import vn.uth.authservice.dto.LoginRequest;
import vn.uth.authservice.dto.RegisterRequest;
import vn.uth.authservice.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

  private final AuthService authService;

  // <-- Thêm constructor này
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody @Validated RegisterRequest req){
    return ResponseEntity.ok(authService.register(req));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody @Validated LoginRequest req){
    return ResponseEntity.ok(authService.login(req));
  }
}
