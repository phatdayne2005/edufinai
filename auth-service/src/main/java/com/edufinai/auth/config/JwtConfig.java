package com.edufinai.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {

    // KEYID CỐ ĐỊNH – PHẢI KHỚP VỚI JWT ĐÃ TẠO
    private static final String FIXED_KEY_ID = "efabffab-8df6-4bf9-85f8-a895757b9c5f";

    @Bean
    public KeyPair keyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate RSA key pair", e);
        }
    }

    @Bean
    public RSAKey rsaKey(KeyPair keyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(FIXED_KEY_ID)  // CỐ ĐỊNH
                .build();
    }

    @Bean
    public JWKSet jwkSet(RSAKey rsaKey) {
        return new JWKSet(rsaKey);
    }

    // THÊM: Method public để expose public key cho SecurityConfig (gen dynamic nếu cần)
    public RSAPublicKey getRsaPublicKey() {
        KeyPair keyPair = keyPair();  // Gen nếu chưa có
        RSAKey rsaKey = rsaKey(keyPair);
        return rsaKey.toRSAPublicKey();
    }
}