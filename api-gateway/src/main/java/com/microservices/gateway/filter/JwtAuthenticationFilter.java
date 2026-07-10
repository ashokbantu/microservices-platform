package com.microservices.gateway.filter;

import com.microservices.gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    private static final List<String> OPEN_ENDPOINTS = List.of(
        "/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/actuator"
    );

    public JwtAuthenticationFilter() {
        super(Config.class);
        this.jwtUtil = null;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if (isOpenEndpoint(path)) return chain.filter(exchange);

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION))
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer "))
                return onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);

            String token = authHeader.substring(7);
            try {
                if (!jwtUtil.validateToken(token))
                    return onError(exchange, "Invalid or expired JWT", HttpStatus.UNAUTHORIZED);

                String username = jwtUtil.extractUsername(token);
                String roles = jwtUtil.extractRoles(token);

                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Name", username)
                    .header("X-User-Roles", roles)
                    .build();

                log.debug("JWT validated for user: {}", username);
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (Exception e) {
                log.error("JWT validation error: {}", e.getMessage());
                return onError(exchange, "JWT validation failed", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        log.warn("Gateway auth error: {}", message);
        return response.setComplete();
    }

    public static class Config {}
}
