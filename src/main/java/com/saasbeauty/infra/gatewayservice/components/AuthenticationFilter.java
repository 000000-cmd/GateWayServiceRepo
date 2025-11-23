package com.saasbeauty.infra.gatewayservice.components;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

@Component
@RefreshScope
public class AuthenticationFilter implements GatewayFilter {

    @Autowired
    private JwtUtil jwtUtil;

    // Lista de rutas públicas
    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/apiV",
            "/api/thirdparties/create",
            "/eureka"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // --- LOG DE DEPURACIÓN ---
        System.out.println(">>> Petición entrante: " + path);

        // Lógica: Si la URL contiene alguno de los endpoints abiertos, NO es segura (es pública)
        Predicate<ServerHttpRequest> isApiSecured = r -> openApiEndpoints.stream()
                .noneMatch(uri -> r.getURI().getPath().contains(uri));

        boolean esPrivada = isApiSecured.test(request);
        System.out.println(">>> ¿Es ruta privada?: " + esPrivada);

        if (esPrivada) {
            // Si entra aquí, el Gateway está exigiendo token
            System.out.println(">>> Validando Token para ruta protegida...");
            if (!request.getHeaders().containsKey("Authorization")) {
                System.out.println(">>> ERROR: No hay header Authorization");
                return this.onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            final String token = this.getAuthHeader(request);

            if (!jwtUtil.validateToken(token)) {
                System.out.println(">>> ERROR: Token inválido o expirado");
                return this.onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
            }

            // Token válido, pasamos datos al microservicio
            Claims claims = jwtUtil.extractAllClaims(token);
            exchange.getRequest().mutate()
                    .header("X-User-Username", claims.getSubject())
                    .build();
        } else {
            System.out.println(">>> Ruta PÚBLICA detectada. Pasando sin token.");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    private String getAuthHeader(ServerHttpRequest request) {
        return request.getHeaders().getOrEmpty("Authorization").get(0).substring(7);
    }
}