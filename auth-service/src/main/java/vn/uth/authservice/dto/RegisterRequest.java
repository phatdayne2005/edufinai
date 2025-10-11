package vn.uth.authservice.dto;

import jakarta.validation.constraints.*;
public record RegisterRequest(
  @NotBlank @Size(min=4,max=50) String username,
  @NotBlank @Size(min=6,max=64) String password,
  @Email @NotBlank String email
) {}
