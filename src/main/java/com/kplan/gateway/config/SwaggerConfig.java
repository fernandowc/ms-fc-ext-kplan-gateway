package com.kplan.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for the API Gateway.
 *
 * The gateway aggregates Swagger documentation from all microservices.
 * Each service exposes its own /v3/api-docs endpoint, and the gateway
 * proxies them via the route definitions in application.yml.
 *
 * The Swagger UI at http://localhost:8080/swagger-ui.html provides a
 * dropdown to select any microservice's API documentation — this is
 * the single entry point for Flutter developers to explore the entire API.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KPlan API Gateway")
                        .description("Unified API gateway for KPlan entertainment discovery platform. "
                                + "Select a microservice from the dropdown to explore its API documentation.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("KPlan Team")
                                .email("dev@kplan.com"))
                        .license(new License()
                                .name("Proprietary")));
    }
}
