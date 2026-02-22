package com.learncicd.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * defaultReplenishRate : 10 requests/sec
 * defaultBurstCapacity : 20 burst capacity
 */
@Configuration
public class RateLimiterConfig {

    @Primary
    @Bean("userServiceRateLimiter")
    public RedisRateLimiter userServiceRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    @Bean("bookmarkRateLimiter")
    public RedisRateLimiter bookmarkRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }

    /*@Bean
    public RedisRateLimiter userServiceRateLimiter() {
        return new RedisRateLimiter(10, 20);
    }*/

    /**
     * Extracts the IP address of the client making the request.
     * Defines per-user or per-IP throttling.
     * HERE PER IP Throttling
     */
    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange ->
                Mono.just(exchange.getRequest()
                        .getRemoteAddress()
                        .getAddress()
                        .getHostAddress());
    }
}
