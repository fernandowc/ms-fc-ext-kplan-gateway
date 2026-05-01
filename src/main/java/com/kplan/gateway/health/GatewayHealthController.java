package com.kplan.gateway.health;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom health endpoint that provides gateway status plus
 * connectivity checks to all downstream microservices.
 *
 * This is useful for:
 * 1. Docker Compose healthcheck (simple /health)
 * 2. Flutter app startup check (is the backend alive?)
 * 3. Operations dashboard (which services are up/down?)
 *
 * The /health/services endpoint is fire-and-forget —
 * if a service doesn't respond in 2s, it's marked as DOWN.
 */
@RestController
@RequiredArgsConstructor
public class GatewayHealthController {

    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "api-gateway");
        response.put("timestamp", Instant.now().toString());
        return Mono.just(response);
    }

    @GetMapping("/health/services")
    public Mono<Map<String, Object>> servicesHealth() {
        WebClient client = WebClient.create();

        Map<String, String> services = Map.of(
                "auth-service", "http://localhost:8081/actuator/health",
                "profile-service", "http://localhost:8082/actuator/health",
                "venue-event-service", "http://localhost:8083/actuator/health",
                "social-group-service", "http://localhost:8084/actuator/health",
                "booking-service", "http://localhost:8085/actuator/health",
                "notification-service", "http://localhost:8086/actuator/health"
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gateway", "UP");
        result.put("timestamp", Instant.now().toString());

        return Mono.just(services.entrySet())
                .flatMapIterable(entries -> entries)
                .flatMap(entry -> client.get()
                        .uri(entry.getValue())
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(2))
                        .map(body -> Map.entry(entry.getKey(), (Object) "UP"))
                        .onErrorReturn(Map.entry(entry.getKey(), "DOWN")))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(servicesStatus -> {
                    result.put("services", servicesStatus);
                    return result;
                });
    }
}
