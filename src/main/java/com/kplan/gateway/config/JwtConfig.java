package com.kplan.gateway.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JWT configuration for the API Gateway.
 * Only loads the PUBLIC key — the gateway never signs tokens, only validates them.
 * This enforces the security principle: auth-service is the sole token issuer.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
@Slf4j
public class JwtConfig {

    private String publicKey;

    @Bean
    public PublicKey jwtPublicKey() {
        try {
            String sanitized = publicKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            // Support both standard and URL-safe Base64 encodings
            String standardBase64 = sanitized
                    .replace('-', '+')
                    .replace('_', '/');
            byte[] keyBytes = Base64.getMimeDecoder().decode(standardBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            log.error("Failed to load JWT public key: {}", e.getMessage());
            throw new IllegalStateException("Cannot initialize JWT public key for gateway", e);
        }
    }
}
