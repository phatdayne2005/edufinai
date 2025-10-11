// src/main/java/vn/uth/authservice/service/AuthService.java
package vn.uth.authservice.service;

import vn.uth.authservice.dto.AuthResponse;
import vn.uth.authservice.dto.LoginRequest;
import vn.uth.authservice.dto.RegisterRequest;

public interface AuthService {
  AuthResponse register(RegisterRequest req);
  AuthResponse login(LoginRequest req);
}
