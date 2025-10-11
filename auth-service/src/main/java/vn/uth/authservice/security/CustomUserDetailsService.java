package vn.uth.authservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import vn.uth.authservice.repository.UserRepository;

@Service @RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository userRepo;
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var u = userRepo.findByUsername(username)
      .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return User.builder()
      .username(u.getUsername())
      .password(u.getPasswordHash())
      .roles(u.getRole().name())
      .disabled(u.getStatus().name().equals("BLOCKED"))
      .build();
  }
}
