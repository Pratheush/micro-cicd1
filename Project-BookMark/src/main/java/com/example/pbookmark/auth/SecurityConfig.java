package com.example.pbookmark.auth;

import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * ✅ Implement Spring Security in auth-service (issue JWT).
 * ✅ Implement GlobalFilter in API Gateway (validate JWT before forwarding).
 * ✅ Configure Spring Security in user-service and bookmark-service to parse JWT claims (username, email, roles).
 * ✅ Use JWT claims directly in user-service (no DB needed).
 * ✅ Forward JWT in Feign calls so downstream services know the user context.
 *
 * ✅ In user-service (and bookmark-service), you just configure Spring Security with a JWT filter (or use spring-boot-starter-oauth2-resource-server) to validate tokens and extract claims.
 * ✅ The gateway does the first validation, and each downstream service can also validate the JWT locally to get username and email from claims.
 *
 * ✅ in user-service you do not need the spring-cloud-gateway dependency or a GlobalFilter.
 * ✅ You don’t call JwtUtil.validateToken() manually in user-service.
 * ✅ In user-service, you don’t need to use your custom JwtUtil class or a GlobalFilter
 *
 * ✅ You simply declare (@AuthenticationPrincipal Jwt jwt) in your controller, and Spring will give you the parsed token.
 * ✅ From that Jwt object, you can directly read jwt.getSubject() (username) and jwt.getClaim("email") (email).
 * 👉 JwtUtil is only useful if you’re manually parsing tokens. With spring-boot-starter-security-oauth2-resource-server,
 *      you don’t need it in user-service — Spring Security does the validation and claim extraction for you.
 *
 * ✅ Claims extraction is done via @AuthenticationPrincipal Jwt jwt in controllers.
 * ✅ Username & email come directly from JWT claims, so user-service doesn’t need a DB.
 * ✅ No GlobalFilter in user-service → that’s only for API Gateway.
 * ✅ JWT validation is handled automatically by Spring Security’s resource server support(DEPENDENCY: spring-boot-starter-security-oauth2-resource-server) (oauth2ResourceServer().jwt() Configured in SecurityConfig)
 *
 * JWT VERIFICATION AND VALIDATION AT DOWNSTREAM SERVICES (user-service & bookmark-service)
 * ✅ Use @AuthenticationPrincipal Jwt jwt in controllers to get username & email directly from JWT claims.
 * ✅ No need for custom JwtUtil or GlobalFilter in downstream services.
 * ✅ Spring Security’s resource server support handles JWT validation and claim extraction.
 *
 * JWT VERIFICATION AND VALIDATION AT DOWNSTREAM SERVICES (user-service & bookmark-service) WAS FAILING EARLIER BUT AT API GATEWAY WAS WORKING FINE :
 * Because your gateway likely validates using:
 * 1. different decoder
 * 2. different algorithm
 * 3. different key length
 * 4. OR custom jjwt parser
 * So gateway accepts token → downstream resource server rejects it.
 *
 * FAILURE WAS HAPPENING BECAUSE OF DIFFERENT ALGORITHM USED IN GATEWAY AND DOWNSTREAM SERVICES.
 * AT AUTH-SERVICE WE ARE USING HS512 ALGORITHM AND AT DOWNSTREAM SERVICES WE WERE USING SOME OTHER ALGORITHM.
 * generating token using: io.jsonwebtoken (jjwt)
 * But validating using: NimbusJwtDecoder.withSecretKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY))).build()
 * SOLUTION :
 * Force Same Algorithm Everywhere in every micro-services
 * HS512 is the algorithm used to sign the JWT : Force Same Algorithm Everywhere in every micro-services when we are validating the token at downstream services using NimbusJwtDecoder
 * SAME ALGORITHM SHOULD BE USED IN AUTH-SERVICE & DOWNSTREAM SERVICES
 * SAME SECRET KEY SHOULD BE USED IN AUTH-SERVICE & DOWNSTREAM SERVICES
 * SAME KEY LENGTH SHOULD BE USED IN AUTH-SERVICE & DOWNSTREAM SERVICES
 *
 * SO ACROSS AUTH-SERVICE & API-GATEWAY & USER-SERVICE & BOOKMARK-SERVICE  DOWNSTREAM SERVICES WE SHOULD USE SAME ALGORITHM, SAME SECRET KEY & SAME KEY LENGTH
 *
 *
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {

    public static final String SECRET_KEY = "UHJhdGhldXNoUkFKUkAyMjc0MTIjQFNHSDE5ODlNaWNyb0NJQ0RPYnNlcnZhYmlsaXR5UGVyZm9ybWFuY2VBbmRBTExLaW5kc09GRi1UZXN0aW5n";

    // The method only contains Spring Security configuration calls that don't throw checked exceptions. so no throws Exception needed
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("SecurityConfig API Request: Security Filter Chain HttpSecurity={}", http );
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        log.info("SecurityConfig : JwtDecoder");
        //return NimbusJwtDecoder.withSecretKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY))).build();

        // Force Same Algorithm Everywhere when validating JWT token in downstream services
        SecretKey key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(SECRET_KEY)
        );

        // Force Same Algorithm Everywhere when validating JWT token in downstream services
        // HS512 is the algorithm used to sign the JWT : Force Same Algorithm Everywhere in every microservices
        return NimbusJwtDecoder.withSecretKey(getKey())
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    // Force Same Algorithm Everywhere when validating JWT token in downstream services
    // HS512 is the algorithm used to sign the JWT : Force Same Algorithm Everywhere in every microservices
    private SecretKey getKey(){
        byte[] decodeKey = Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(decodeKey);

    }

}
