package com.learncicd.apigateway.filter;

import com.learncicd.apigateway.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * it is a GatewayFilterFactory, not a global filter — so it must be attached to routes via the RouteLocatorBuilder filters() DSL.
 *  throwing exceptions inside a reactive chain (not ideal for Gateway)
 *
 *  ✅ Best Practice for Gateway Filters
 * Since Spring Cloud Gateway is reactive, throwing exceptions inside the filter chain is discouraged. Instead, you should:
 * Set the response status (e.g., 401 Unauthorized).
 * Write a response body with an error message.
 * Return Mono<Void> to terminate the chain gracefully.
 * That’s exactly what your unauthorized() helper method does — it avoids breaking the reactive pipeline with exceptions.
 */

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final JwtUtil jwtUtil;

    private final Validator validator;

    public AuthFilter(JwtUtil jwtUtil, Validator validator){
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.validator = validator;
    }


    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if(validator.predicate.test(exchange.getRequest())) {
                if(!exchange.getRequest().getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)){
                    throw new BadRequestException("Authorization header is missing", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                //String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0); // This can throw NPE(NullPointerException) or IndexOutOfBounds.
                String token = null;
                if(null != authHeader && authHeader.startsWith("Bearer ")){
                    token = authHeader.substring(7);
                }
                try{
                    jwtUtil.validateToken(token);
                }catch (Exception e){
                    throw new BadRequestException("Invalid token", HttpStatus.UNAUTHORIZED);
                }
            }
            return chain.filter(exchange);
        };
    }

    public static class Config{

    }
}
