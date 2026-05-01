package com.kplan.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate limiter configuration using Redis-backed token bucket algorithm.
 *
 * Strategy:
 * 1. Authenticated users: rate limit by userId (from X-User-Id header)
 * 2. Anonymous users: rate limit by IP address
 *
 * This prevents:
 * - API abuse from unauthenticated clients (brute force login, scraping)
 * - A single user from overwhelming the system
 *
 * The actual rate limits (replenish rate, burst capacity) are configured
 * per-route in application.yml using the RequestRateLimiter filter.
 *
 * Redis is required for distributed rate limiting across gateway instances.
 * Without Redis, this degrades gracefully (no rate limiting applied).
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Resolves the rate limit key:
     * - If X-User-Id header present (JWT was valid) → userId
     * - Otherwise → client IP address
     */
    @Bean
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }

            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
