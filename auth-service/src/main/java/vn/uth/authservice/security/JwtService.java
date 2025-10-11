package vn.uth.authservice.security;

import io.jsonwebtoken.Jwts; import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders; import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key; import java.time.Instant; import java.util.Date; import java.util.Map;

@Service
public class JwtService {
  @Value("${jwt.secret}") private String secret;
  @Value("${jwt.access-exp-minutes}") private long expMinutes;
  @Value("${jwt.issuer}") private String issuer;

  private Key key(){ return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)); }

  public String generateToken(String subject, Map<String,Object> claims){
    Instant now = Instant.now();
    return Jwts.builder()
      .setClaims(claims)
      .setSubject(subject)
      .setIssuer(issuer)
      .setIssuedAt(Date.from(now))
      .setExpiration(Date.from(now.plusSeconds(expMinutes*60)))
      .signWith(key(), SignatureAlgorithm.HS256)
      .compact();
  }

  public String extractSubject(String token){
    return Jwts.parserBuilder().setSigningKey(key()).build()
      .parseClaimsJws(token).getBody().getSubject();
  }
}
