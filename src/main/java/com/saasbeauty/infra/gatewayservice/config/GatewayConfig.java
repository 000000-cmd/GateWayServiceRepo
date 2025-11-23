package com.saasbeauty.infra.gatewayservice.config;

import com.saasbeauty.infra.gatewayservice.components.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**", "/api/users/**", "/api/lists/roles/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("lb://auth-service"))

                .route("location-service", r -> r.path("/api/locations/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("lb://location-service"))

                .route("thirdparty-service", r -> r.path("/api/thirdparties/**", "/api/lists/document-types/**", "/api/lists/gender-types/**")
                        .filters(f -> f.filter(authenticationFilter))
                        .uri("lb://thirdparty-service"))

                .build();
    }
}