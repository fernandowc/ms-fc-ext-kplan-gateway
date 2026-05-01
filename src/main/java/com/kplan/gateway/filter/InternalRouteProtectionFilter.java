package com.kplan.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Blocks any external request attempting to reach /internal/** endpoints.
 *
 * Internal endpoints are designed for service-to-service communication
 * within the Docker network. They use network isolation as their security
 * boundary (no JWT required). The gateway must NEVER forward these requests
 * from external clients.
 *
 * Examples of internal endpoints:
 * - POST /internal/profiles (called by auth-service after registration)
 * - POST /internal/notifications/send (called by other services)
 */
@Component
@Slf4j
public class InternalRouteProtectionFilter implements GlobalFilter, Ordered {

    private static final String INTERNAL_PATH_PREFIX = "/internal";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.contains(INTERNAL_PATH_PREFIX)) {
            log.warn("Blocked external access to internal endpoint: {} from {}",
                    path,
                    exchange.getRequest().getRemoteAddress());

            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run BEFORE JWT filter — reject immediately, don't waste resources
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
