package com.learncicd.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * it is a GatewayFilterFactory, not a global filter — so it must be attached to routes via the RouteLocatorBuilder filters() DSL.
 * throwing exceptions inside a reactive chain (not ideal for Gateway)
 *
 * ✅ Why This Version Is Correct for Gateway ::
 * ✔ uses WebFlux HttpHeaders API correctly
 * ✔ no blocking exceptions thrown
 * ✔ reactive error response returned
 * ✔ null-safe header parsing
 * ✔ constructor injection (preferred)
 * ✔ gateway-grade error handling
 */
@Component
public class AuthGPTFilter extends AbstractGatewayFilterFactory<AuthGPTFilter.Config> {

    private final JwtUtil jwtUtil;
    private final Validator validator;

    public AuthGPTFilter(JwtUtil jwtUtil, Validator validator) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.validator = validator;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            if (!validator.predicate.test(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            HttpHeaders headers = exchange.getRequest().getHeaders();
            if (!headers.containsHeader(HttpHeaders.AUTHORIZATION)) {
                return unauthorized(exchange, "Authorization header is missing");
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
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    public static class Config { }
}
