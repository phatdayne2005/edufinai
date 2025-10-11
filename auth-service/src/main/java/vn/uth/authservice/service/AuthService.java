package vn.uth.authservice.service;

import vn.uth.authservice.dto.*; 

public interface AuthService {
  AuthResponse register(RegisterRequest req);
  AuthResponse login(LoginRequest req);
}
