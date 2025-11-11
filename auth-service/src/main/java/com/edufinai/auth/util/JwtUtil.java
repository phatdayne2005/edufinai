package com.edufinai.auth.util;

import com.edufinai.auth.config.JwtConfig;
import com.edufinai.auth.model.UserRole;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final RSAKey rsaKey;

    private static final String ISSUER = "edufinai-auth-service";
    private static final long ACCESS_EXP = 86400L; // 24h
    private static final long REFRESH_EXP = 604800L; // 7 days

    // GENERATE ACCESS TOKEN
    public String generateAccessToken(String username, UUID userId, UserRole role, String email) {
        return generateToken(username, userId, role, email, ACCESS_EXP, false);
    }

    // GENERATE REFRESH TOKEN
    public String generateRefreshToken(String username, UUID userId, UserRole role) {
        return generateToken(username, userId, role, null, REFRESH_EXP, true);
    }

    private String generateToken(String username, UUID userId, UserRole role, String email, long expSeconds, boolean isRefresh) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(username)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(expSeconds)))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", userId.toString())
                .claim("role", role.name());

        if (!isRefresh && email != null) {
            claims.claim("email", email);
        }
        if (isRefresh) {
            claims.claim("type", "refresh");
        }

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims.build());
        try {
            signedJWT.sign(new RSASSASigner(rsaKey.toRSAPrivateKey()));
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    // VALIDATE TOKEN
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .requireIssuer(ISSUER)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // EXTRACT USERNAME
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // EXTRACT ROLE
    public UserRole extractRole(String token) {
        String role = extractClaim(token, claims -> claims.get("role", String.class));
        return UserRole.valueOf(role);
    }

    // EXTRACT USER ID
    public UUID extractUserId(String token) {
        String userId = extractClaim(token, claims -> claims.get("userId", String.class));
        return UUID.fromString(userId);
    }

    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .requireIssuer(ISSUER)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);
    }

    // GET EXPIRATION (giây)
    public long getExpirationTime() {
        return ACCESS_EXP;
    }
}