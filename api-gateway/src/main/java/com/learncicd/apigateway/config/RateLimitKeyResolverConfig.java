package com.learncicd.apigateway.config;

import com.learncicd.apigateway.filter.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Base64;

/**
 * using a JWT-based KeyResolver that extracts the username (or subject) from the token already present
 * in the Authorization header. This is the production-grade approach when your gateway validates JWT.
 * Thus Rate limit per authenticated user (username from JWT) So each logged-in user gets their own request quota.
 * ✅ How it works (simple flow)
 * For every request at API Gateway::::
 * 1. Read Authorization header
 * 2. Extract Bearer token
 * 3. Decode JWT
 * 4. Read username (sub or preferred_username claim)
 * 5. Use that as RateLimiter key
 * 6. Make sure JWT token has claims set up with username in it when JWT is generated
 *
 * For every request at API Gateway:
 * 1. Extracts the username from the JWT token in the Authorization header.
 * 2. Uses this username as the key for rate limiting.
 * 3. Checks the Redis cache for the user's request quota.
 * 4. If the quota is exceeded, returns a 429 (Too Many Requests) response.
 * 5. If the quota is not exceeded, allows the request to proceed.
 *
 * API Gateway validates JWT before rate limiting Which we are already doing with our AuthGlobalFilter JWT auth filter
 *
 * This bean extracts username (subject) from JWT and becomes the rate-limit key.
 */

@Configuration
@Slf4j
public class RateLimitKeyResolverConfig {

    @Primary
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
        log.info("userKeyResolver Bean is created");
        return exchange -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Mono.just("anonymous");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                        //.parserBuilder()
                        .verifyWith(
                                Keys.hmacShaKeyFor(
                                        Base64.getDecoder().decode(JwtUtil.SECRET_KEY)
                                )
                        )
                        /*.setSigningKey(
                                Keys.hmacShaKeyFor(
                                        Base64.getDecoder().decode(JwtUtil.SECRET_KEY)
                                )
                        )*/
                        .build()
                        //.parseClaimsJws(token)
                        .parseSignedClaims(token)
                        //.getBody();
                        .getPayload();

                String username = claims.get("username").toString(); // usually username
                return Mono.just(username);

            } catch (Exception e) {
                return Mono.just("invalid-token");
            }
        };
    }
}
