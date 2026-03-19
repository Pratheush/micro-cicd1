package com.learncicd.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * ✅ Forward JWT in Feign calls so downstream services know the user context.
 * When user-service calls bookmark-service: Forward the same JWT using a Feign interceptor:
 * This way, bookmark-service also sees the logged-in user context.
 *
 * 🔹 Feign Interceptor
 * ✅ A Feign RequestInterceptor runs before every Feign client call.
 * ✅ It lets you modify the outgoing request (e.g., add headers).
 * ✅ In your FeignAuthInterceptor, you grab the current Authentication from SecurityContextHolder and forward the JWT in the Authorization header.
 * ✅ This ensures that when user-service calls bookmark-service, the same JWT is passed along, so bookmark-service knows who the logged-in user is.
 * ✅ 👉 Interceptors in Feign only affect requests made by Feign clients. They don’t touch responses.
 *
 * 🔹 Filters vs Interceptors
 * ✅ Filters (like Spring Security filters or Gateway filters) decide whether a request is allowed or blocked, often based on conditions (e.g., JWT validity).
 * ✅ Interceptors (like Feign’s RequestInterceptor) modify or enrich requests/responses — e.g., adding headers, logging, or retry logic.
 *
 *  In Spring Security resource server mode:
 *  ✅ Spring Security’s resource server support handles JWT validation and claim extraction.
 *  ✅ You don’t need a custom JwtUtil or GlobalFilter in user-service.
 *  ✅ Claims extraction is done via @AuthenticationPrincipal Jwt jwt in controllers.
 *  ✅ Username & email come directly from JWT claims, so user-service doesn’t need a DB.
 *  ✅ No GlobalFilter in user-service → that’s only for API Gateway.
 *  ✅ JWT validation is handled automatically by Spring Security’s resource server support.
 *
 *  In user-service, you don’t need to manually parse tokens or use a GlobalFilter.
 *  In Spring Security resource server mode:
 *  Authentication = JwtAuthenticationToken
 *  principal = Jwt
 *  credentials = Jwt   ← NOT String
 *
 *  Make sure this is the import: : import org.springframework.security.oauth2.jwt.Jwt;
 *  Only Spring Security Jwt has: getTokenValue() , getSubject() , getClaims(), getExpiresAt()
 *
 */
@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String tokenValue = jwt.getTokenValue();
            template.header("Authorization", "Bearer " + tokenValue);
        }

        // ✅ If You Want Even More Precise Typing we can bind directly to the authentication type Spring creates: This avoids any ambiguity.
        /*if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            template.header("Authorization",
                    "Bearer " + jwtAuth.getToken().getTokenValue());
        }*/
    }

    /**
     * Feign client is forwarding the Authorization header from User-Service to Project-Bookmark through @Bean.
     */
    /*@Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getCredentials() instanceof String token) {
                template.header("Authorization", "Bearer " + token);
            }
        };
    }*/

}
