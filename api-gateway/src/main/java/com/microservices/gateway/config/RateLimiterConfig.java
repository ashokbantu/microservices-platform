package com.microservices.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

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

    @Bean
    public RedisRateLimiter globalRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    @Bean
    public RedisRateLimiter authRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }
}
