package com.kplan.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive security configuration for the API Gateway.
 *
 * Key design decisions:
 * 1. CSRF disabled — stateless API consumed by mobile app (Flutter)
 * 2. All requests permitted at gateway level — authorization is delegated
 *    to each downstream microservice via their own SecurityConfig
 * 3. JWT validation happens in JwtAuthenticationGlobalFilter (not here)
 *    because the gateway's role is to FORWARD identity, not to ENFORCE access
 * 4. Form login and HTTP basic disabled — pure API gateway
 *
 * Why not enforce auth at gateway?
 * - Each service knows its own public vs protected endpoints
 * - The gateway would need to maintain a centralized route-to-permission map
 *   that duplicates logic and becomes a maintenance burden
 * - The gateway validates JWT and propagates X-User-Id — that's sufficient
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                .build();
    }
}
