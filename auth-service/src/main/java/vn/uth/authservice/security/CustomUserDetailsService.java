// src/main/java/vn/uth/authservice/security/CustomUserDetailsService.java
package vn.uth.authservice.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.uth.authservice.entity.User;
import vn.uth.authservice.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepo;

  public CustomUserDetailsService(UserRepository userRepo) {
    this.userRepo = userRepo;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User u = userRepo.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return org.springframework.security.core.userdetails.User // <-- dùng FQCN
        .withUsername(u.getUsername())
        .password(u.getPasswordHash())
        .roles(u.getRole().name())
        .disabled(u.getStatus().name().equals("BLOCKED"))
        .build();
  }
}
