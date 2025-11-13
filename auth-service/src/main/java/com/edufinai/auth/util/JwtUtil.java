package com.edufinai.auth.util;

import com.edufinai.auth.config.JwtConfig;
import com.edufinai.auth.model.UserRole;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final RSAKey rsaKey;

    // PUBLIC STATIC ISSUER
    public static final String ISSUER = "edufinai-auth-service";

    private static final long ACCESS_EXP = 86400L;  // 24h
    private static final long REFRESH_EXP = 604800L; // 7 days

    // GETTER CHO RSA KEY (dùng trong AuthenticationService)
    public RSAKey getRsaKey() {
        return rsaKey;
    }

    // GENERATE ACCESS TOKEN
    public String generateAccessToken(String username, UUID userId, UserRole role, String email) {
        return generateToken(username, userId, role, email, ACCESS_EXP, false);
    }

    // GENERATE REFRESH TOKEN → DÙNG JWT, KHÔNG UUID
    public String generateRefreshToken(String username, UUID userId, UserRole role) {
        return generateToken(username, userId, role, null, REFRESH_EXP, true);
    }

    private String generateToken(String username, UUID userId, UserRole role, String email, long expSeconds, boolean isRefresh) {
        Instant now = Instant.now();

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(username)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(expSeconds)))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", userId.toString())
                .claim("role", role.name());

        if (!isRefresh && email != null) {
            claimsBuilder.claim("email", email);
        }
        if (isRefresh) {
            claimsBuilder.claim("type", "refresh");
        }

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsBuilder.build());

        try {
            // SỬA: Bắt JOSEException
            RSASSASigner signer = new RSASSASigner(rsaKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (com.nimbusds.jose.JOSEException e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    // VALIDATE TOKEN
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(rsaKey.toRSAPublicKey())
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

    // EXTRACT TYPE (refresh/access)
    public String extractType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> resolver) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(rsaKey.toRSAPublicKey())
                    .requireIssuer(ISSUER)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return resolver.apply(claims);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    // GET EXPIRATION (access token)
    public long getExpirationTime() {
        return ACCESS_EXP;
    }
}