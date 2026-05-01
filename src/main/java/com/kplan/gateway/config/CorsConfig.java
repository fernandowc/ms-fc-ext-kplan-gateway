package com.kplan.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Programmatic CORS configuration for the API Gateway.
 *
 * This complements the YAML-based CORS config in application.yml.
 * Using a programmatic bean gives us more control and ensures
 * CORS headers are applied consistently even for error responses.
 *
 * For MVP (Flutter mobile app), we allow all origins.
 * In production, this should be restricted to:
 * - The Flutter app's web domain (if deployed as PWA)
 * - Admin panel domain
 * - Specific development environments
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // MVP: allow all origins (Flutter mobile doesn't send Origin header anyway)
        // Production: restrict to specific domains
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(
                "Authorization",
                "X-Total-Count",
                "X-Total-Pages",
                "X-Request-Id"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Pre-flight cache: 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
