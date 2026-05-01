package com.kplan.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.PublicKey;

/**
 * Global reactive filter for JWT validation at the gateway level.
 *
 * Architecture decisions:
 * 1. Validates JWT signature with RSA public key (stateless, no auth-service call)
 * 2. Extracts userId and role from claims
 * 3. Propagates X-User-Id and X-User-Role headers downstream
 * 4. Does NOT block requests without JWT — each downstream service decides
 *    which endpoints require authentication via its own SecurityConfig
 * 5. Strips any incoming X-User-Id/X-User-Role headers to prevent spoofing
 *
 * Order: runs BEFORE routing (Ordered.HIGHEST_PRECEDENCE + 1)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final PublicKey jwtPublicKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Step 1: Always strip incoming identity headers to prevent spoofing
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_ROLE);
                });

        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);

        // Step 2: If no Bearer token present, forward request without identity headers
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }

        // Step 3: Validate JWT and extract claims
        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtPublicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // Step 4: Propagate identity headers downstream
            requestBuilder.header(HEADER_USER_ID, userId);
            if (role != null) {
                requestBuilder.header(HEADER_USER_ROLE, role);
            }

            log.debug("JWT validated for user [{}] with role [{}], path: {}",
                    userId, role, exchange.getRequest().getPath());

        } catch (Exception e) {
            // Invalid/expired token — don't block, let downstream service decide
            // This allows public endpoints to work even with malformed tokens
            log.debug("Invalid JWT token at gateway: {}", e.getMessage());
        }

        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    @Override
    public int getOrder() {
        // Run before the routing filter but after Netty channel handlers
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
