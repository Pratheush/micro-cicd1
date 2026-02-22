package com.learncicd.apigateway.filter;

import jakarta.annotation.Nonnull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * if you want authentication applied to every request automatically, use a GlobalFilter instead of AbstractGatewayFilterFactory.
 * This is the correct pattern when:
 * ✅ JWT must be validated for most routes
 * ✅ You don’t want to attach filter per-route
 * ✅ You want central gateway security enforcement
 * ✅ You only bypass a small whitelist (login, eureka UI, health, etc.)
 *
 * GlobalFilter runs automatically for all routes so remove this filter configuration-setup from all routes:  .filters(f -> f.filter(authFilter.apply(...)))
 *
 * ✅ When To Use Each Filter Type
 * Use GlobalFilter when:
 * 1. auth required almost everywhere
 * 2. security is cross-cutting concern
 * 3. centralized gateway security
 *
 * Use GatewayFilterFactory when:
 * 1. route-specific behavior
 * 2. some routes need auth, some don’t
 * 3. filter has config parameters
 *
 * ✅ Execution Order Control
 * getOrder() decides priority:
 * -1  → before most filters
 * 0   → default
 * +1  → later
 * Auth filters should run early → use negative order.
 *
 */

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final Validator validator;

    public AuthGlobalFilter(JwtUtil jwtUtil, Validator validator) {
        this.jwtUtil = jwtUtil;
        this.validator = validator;
    }

    @Override
    @Nonnull
    public Mono<Void> filter(ServerWebExchange exchange,@Nonnull GatewayFilterChain chain) {

        if (!validator.predicate.test(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        HttpHeaders headers = exchange.getRequest().getHeaders();

        if (!headers.containsHeader(HttpHeaders.AUTHORIZATION)) {
            return unauthorized(exchange, "Authorization header missing");
        }

        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Invalid Authorization format");
        }

        String token = authHeader.substring(7);

        try {
            jwtUtil.validateToken(token);
        } catch (Exception e) {
            return unauthorized(exchange, "Invalid token");
        }

        return chain.filter(exchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(msg.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // run early
    }
}
