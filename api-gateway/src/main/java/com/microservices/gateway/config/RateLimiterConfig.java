package com.microservices.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * Rate limit key: authenticated user from JWT header, fallback to IP address.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String user = exchange.getRequest().getHeaders().getFirst("X-User-Name");
            if (user != null && !user.isBlank()) return Mono.just(user);
            String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
            return Mono.just(ip);
        };
    }

    /**
     * Default rate limiter: 100 req/sec, burst of 200.
     * Marked @Primary so Spring Cloud Gateway auto-wires this one
     * when no explicit bean name is specified in route filters.
     */
    @Bean
    @Primary
    public RedisRateLimiter globalRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    /**
     * Strict rate limiter for auth endpoints: 10 req/sec, burst of 20.
     * Referenced explicitly by name "#{@authRateLimiter}" in YAML routes.
     */
    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
}
