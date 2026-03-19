package com.learncicd.apigateway.config.security;


import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;

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
 *  HERE RBAC (Role-Based Access Control) is used to enforce access control policies based on user roles.
 *  ON API-GATEWAY LEVEL WE ARE ENFORCING ACCESS CONTROL POLICIES BASED ON USER ROLES OVER ALL MICRO-SERVICES API ENDPOINTS ROUTES
 *  JR → only GET allowed.
 *  SR → GET + POST allowed.
 *  TL → GET + POST + PUT allowed.
 *  ADMIN → unrestricted access to all bookmark endpoints.
 *  This is enforced at gateway level, so requests are blocked before reaching services.
 *  JWT must carry the correct roles claim (ROLE_JR, ROLE_SR, etc.), which Spring Security maps to hasRole() checks.
 *
 *  IN REACTIVE API GATEWAY WE ARE USING DIFFERENTLY :
 *  | Servlet (Wrong for you)   | Reactive (Correct)         |
 * | ------------------------- | -------------------------- |
 * | `@EnableWebSecurity`      | `@EnableWebFluxSecurity`   |
 * | `SecurityFilterChain`     | `SecurityWebFilterChain`   |
 * | `HttpSecurity`            | `ServerHttpSecurity`       |
 * | `JwtDecoder`              | `ReactiveJwtDecoder`       |
 * | `NimbusJwtDecoder`        | `NimbusReactiveJwtDecoder` |
 * | `authorizeHttpRequests()` | `authorizeExchange()`      |
 *
 */
@Configuration
@EnableWebFluxSecurity
@Slf4j
public class SecurityConfig {


    public static final String SECRET_KEY = "UHJhdGhldXNoUkFKUkAyMjc0MTIjQFNHSDE5ODlNaWNyb0NJQ0RPYnNlcnZhYmlsaXR5UGVyZm9ybWFuY2VBbmRBTExLaW5kc09GRi1UZXN0aW5n";

    private final String BOOKMARK_LINKS = "/api/bookmarks/**";
    private final String USER_LINKS = "/api/users/**";


    // The method only contains Spring Security configuration calls that don't throw checked exceptions. so no throws Exception needed
    //@Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("SecurityConfig API Request: Security Filter Chain HttpSecurity={}", http );

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Register Access Denied Handler Bean:
                .exceptionHandling(exceptionHandlingSpec ->
                        exceptionHandlingSpec.accessDeniedHandler(accessDeniedHandler()))
                // Register Authentication Entry Point Handler Bean:
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
                        .authorizeExchange(exchange -> exchange

                                // ✅ Correct Ordering
                                //Always define from most privileged to least:
                                        /**
                                         * 🎯 Best Practice
                                         * Keep public endpoints separate with .permitAll().
                                         * Use .hasAnyRole() for shared access paths.
                                         * Reserve .hasRole() for exclusive access (like POST → USER only).
                                         * Avoid repeating the same path multiple times with different roles — that’s what caused the 403 issue.
                                         *  replace the individual role matchers with these consolidated .hasAnyRole() rules. It will Prevent ordering conflicts.
                                         */

                                // Public endpoints
                                .pathMatchers("/auth/register-user","/auth/generate-token","/eureka/web","/eureka/**").permitAll()

                                        /**
                                         * Rule ordering matters
                                         * Spring Security processes rules in the order you declare them. The first match wins.
                                         * So if you put .hasRole("USER") before .hasRole("ADMIN"), then an ADMIN request to /api/bookmarks/**
                                         * matches the USER rule first. Since the token doesn’t contain ROLE_USER, access is denied — even though
                                         * an ADMIN rule exists later.
                                         */
                                // USER → full access (GET, PUT, DELETE)
//                                .pathMatchers(HttpMethod.GET,BOOKMARK_LINKS,USER_LINKS).hasRole("USER")
//                                .pathMatchers(HttpMethod.POST,BOOKMARK_LINKS,USER_LINKS).hasRole("USER")
//                                .pathMatchers(HttpMethod.PUT,BOOKMARK_LINKS,USER_LINKS).hasRole("USER")
//                                .pathMatchers(HttpMethod.DELETE,BOOKMARK_LINKS,USER_LINKS).hasRole("USER")

                                        /**
                                         * uncomment this block of code if you want to use RoleHierarchy Bean that we created here at downside
                                         * uncomment RoleHierarchy too to use this block of code for matching rule :
                                         */
                                .pathMatchers(BOOKMARK_LINKS,USER_LINKS).hasRole("USER")

                                // ADMIN → full access (GET, PUT, DELETE)
                                .pathMatchers(HttpMethod.GET,BOOKMARK_LINKS,USER_LINKS).hasRole("ADMIN")
                                .pathMatchers(HttpMethod.PUT,BOOKMARK_LINKS,USER_LINKS).hasRole("ADMIN")
                                .pathMatchers(HttpMethod.DELETE,BOOKMARK_LINKS,USER_LINKS).hasRole("ADMIN")

                                // TL → READ + WRITE + UPDATE (GET, PUT)
                                .pathMatchers(HttpMethod.GET, BOOKMARK_LINKS,USER_LINKS).hasRole("TL")
                                .pathMatchers(HttpMethod.PUT, BOOKMARK_LINKS,USER_LINKS).hasRole("TL")

                                // JR → only READ (GET)
                                .pathMatchers(HttpMethod.GET, BOOKMARK_LINKS,USER_LINKS).hasRole("JR")

                                /*// GET → accessible by all roles (USER, ADMIN, TL, JR).
                                .pathMatchers(HttpMethod.GET,BOOKMARK_LINKS,USER_LINKS).hasAnyRole("USER","ADMIN","TL","JR")
                                // PUT → accessible by USER, ADMIN, TL.
                                .pathMatchers(HttpMethod.PUT,BOOKMARK_LINKS,USER_LINKS).hasAnyRole("USER","ADMIN","TL")
                                // DELETE → accessible by USER, ADMIN.
                                .pathMatchers(HttpMethod.DELETE,BOOKMARK_LINKS,USER_LINKS).hasAnyRole("USER","ADMIN")
                                // POST → accessible only by USER.
                                .pathMatchers(HttpMethod.POST,BOOKMARK_LINKS,USER_LINKS).hasRole("USER")*/

                                // Any other request must be authenticated
                                .anyExchange().authenticated()
                        ).oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                            jwt.jwtDecoder(jwtDecoder());
                            jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()); // JwtAuthenticationConverter is added

                }));
        return http.build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder() {
        log.info("SecurityConfig : JwtDecoder");
        //return NimbusJwtDecoder.withSecretKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY))).build();

        // Force Same Algorithm Everywhere when validating JWT token in downstream services
        SecretKey key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(SECRET_KEY)
        );

        // Force Same Algorithm Everywhere when validating JWT token in downstream services
        // HS512 is the algorithm used to sign the JWT : Force Same Algorithm Everywhere in every microservices
        return NimbusReactiveJwtDecoder.withSecretKey(getKey())
                .macAlgorithm(MacAlgorithm.HS512)    // enforce same algorithm everywhere
                .build();
    }

    // Force Same Algorithm Everywhere when validating JWT token in downstream services
    // HS512 is the algorithm used to sign the JWT : Force Same Algorithm Everywhere in every microservices
    private SecretKey getKey(){
        byte[] decodeKey = Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(decodeKey);

    }

    /**
     * explicitly configured a converter to map "authorities" → GrantedAuthority objects.
     * Spring will NOT map "authorities" → GrantedAuthority objects automatically
     * If not mapped: >> Authentication.getAuthorities() = []
     * Then: hasAuthority('BOOKMARK_READ') = FALSE , Even though it exists in the token.
     *
     * JwtAuthenticationConverter should be In the project-bookmark service (the resource server) —
     * because that is where Spring Security must convert the incoming JWT into Authentication
     * with GrantedAuthority for @PreAuthorize to work.
     * @return
     *
     *
     * JWT contains: "roles": "ROLE_ADMIN"
     * so Converter should map "roles" : "ROLE_ADMIN" And "authorities": [ "BOOKMARK_READ", ... ] into GrantedAuthorities.
     * So inside Spring Security:
     * Authentication.getAuthorities() = [ROLE_ADMIN, BOOKMARK_READ, BOOKMARK_UPDATE, BOOKMARK_DELETE, BOOKMARK_WRITE]
     * So .hasRole("JR") will work Because hasRole("JR") internally checks for: ROLE_JR
     *
     * Now Authentication will contain:
     * ROLE_ADMIN/ROLE_TL/ROLE_JR/ROLE_USER
     * BOOKMARK_READ
     * BOOKMARK_UPDATE
     * BOOKMARK_DELETE
     */
    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");   // important: no ROLE_ prefix

        ReactiveJwtAuthenticationConverter reactiveConverter = new ReactiveJwtAuthenticationConverter();

        reactiveConverter.setJwtGrantedAuthoritiesConverter(jwt ->
                {
                    Collection<GrantedAuthority> authorities = new ArrayList<>(authoritiesConverter.convert(jwt));
                    String role= jwt.getClaimAsString("roles");
                    if(role!=null){
                        authorities.add(new SimpleGrantedAuthority(role));
                    }
                    return Flux.fromIterable(authorities);
                }
        );

        return reactiveConverter;
    }

    /**
     * Log Access Denied FOR SECURITY AUDIT
     * Handles cases where authentication succeeded but authorization failed (user doesn’t have required role).
     * Logs a warning audit entry:
     * - If authenticated: user, roles, method, path.
     * - If anonymous: method, path.
     * Responds with 403 Forbidden.
     * 👉 This gives you visibility into who was denied access and why.
     * @return
     */
    @Bean
    public ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, denied) -> {

            return exchange.getPrincipal()
                    .cast(Authentication.class)
                    .switchIfEmpty(Mono.justOrEmpty(null)) // safer handling
                    //.defaultIfEmpty(null) // this null gave me null pointer exception :
                    .flatMap(auth -> {

                        if (auth != null) {
                            log.info("INFO ACCESS_DENIED user={} roles={} method={} path={}",
                                    auth.getName(),
                                    auth.getAuthorities(),
                                    exchange.getRequest().getMethod(),
                                    exchange.getRequest().getURI().getPath());

                            log.warn("WARN ACCESS_DENIED user={} roles={} method={} path={}",
                                    auth.getName(),
                                    auth.getAuthorities(),
                                    exchange.getRequest().getMethod(),
                                    exchange.getRequest().getURI().getPath());

                        } else {
                            log.info("INFO ACCESS_DENIED anonymous method={} path={}",
                                    exchange.getRequest().getMethod(),
                                    exchange.getRequest().getURI().getPath());

                            log.warn("WARN ACCESS_DENIED anonymous method={} path={}",
                                    exchange.getRequest().getMethod(),
                                    exchange.getRequest().getURI().getPath());

                        }

                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    /**
     * 1. Log Unauthorized (Invalid / Missing JWT) For Security AUDIT
     * 2. Handles cases where authentication fails (invalid/missing JWT).
     * 3. Logs a warning audit entry:
     *     - Method
     *     - Path
     *     - Reason (exception message)
     * 4. Responds with 401 Unauthorized.
     * 👉 This ensures you have a clear audit trail whenever someone tries to access without valid credentials.
     * @return
     */
    @Bean
    public ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, ex) -> {

            log.info("INFO c={} path={} reason={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath(),
                    ex.getMessage()
            );

            log.warn("WARN c={} path={} reason={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath(),
                    ex.getMessage()
            );

            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        };
    }


    /**
     * 🎯 Practical Effect
     * ROLE_USER → can GET, DELETE, and (via PBAC) create bookmarks.
     * ROLE_ADMIN → can GET, PUT, DELETE.
     * ROLE_TL → can GET, PUT.
     * ROLE_SR/JR → only GET (depending on hierarchy setup).
     * PBAC inside the bookmark service enforces ownership rules (e.g., USER can only update/delete their own).
      * @param http
     * @param roleHierarchy
     * @return
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            RoleHierarchy roleHierarchy) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(exceptionHandlingSpec ->
                        exceptionHandlingSpec.accessDeniedHandler(accessDeniedHandler()))
                // Register Authentication Entry Point Handler Bean:
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/register-user","/auth/generate-token","/eureka/**").permitAll()
                        .pathMatchers(HttpMethod.GET, BOOKMARK_LINKS, USER_LINKS)
                        .access(authorizationManager(roleHierarchy, "ROLE_USER","ROLE_ADMIN","ROLE_TL","ROLE_JR"))
                        .pathMatchers(HttpMethod.PUT, BOOKMARK_LINKS, USER_LINKS)
                        .access(authorizationManager(roleHierarchy, "ROLE_USER","ROLE_ADMIN", "ROLE_TL"))
                        .pathMatchers(HttpMethod.DELETE, BOOKMARK_LINKS, USER_LINKS)
                        .access(authorizationManager(roleHierarchy, "ROLE_ADMIN", "ROLE_USER"))
                        .pathMatchers(HttpMethod.POST,BOOKMARK_LINKS,USER_LINKS)
                        .access(authorizationManager(roleHierarchy,"ROLE_USER"))
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                    jwt.jwtDecoder(jwtDecoder());
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()); // JwtAuthenticationConverter is added

                }))
                .build();
    }

    private ReactiveAuthorizationManager<AuthorizationContext> authorizationManager(
            RoleHierarchy roleHierarchy, String... roles) {

        return (authentication, context) ->
                authentication.map(auth -> {
                    Collection<? extends GrantedAuthority> authorities =
                            roleHierarchy.getReachableGrantedAuthorities(auth.getAuthorities());

                    for (String role : roles) {
                        if (authorities.contains(new SimpleGrantedAuthority(role))) {
                            return new AuthorizationDecision(true);
                        }
                    }
                    return new AuthorizationDecision(false);
                });
    }





    /**
     * ✅ Practical Effect
     * ROLE_USER becomes the most privileged role.
     * A user with ROLE_USER will implicitly have ADMIN, TL, SR, and JR privileges.
     * That means USERs can do everything that ADMINs, TLs, SRs, and JRs can do.
     * ROLE_ADMIN is weaker than USER.
     * ADMIN inherits TL, SR, JR, but does not inherit USER.
     * ROLE_TL inherits SR and JR.
     * ROLE_SR inherits JR.
     * ROLE_JR is the least privileged.
     *
     * 🎯 Alignment With Intent
     * ✅ Create bookmark → only ROLE_USER (handled at service level).
     * ✅ Read bookmark → any role can read, but PBAC ensures USER only sees their own.
     * ✅ Delete bookmark → allowed if USER owns it, or ADMIN (PBAC enforces ownership check).
     * ✅ Update bookmark → allowed for ADMIN, TL, or USER who owns it (PBAC enforces ownership check).
     *
     * So your gateway RBAC + bookmark PBAC combination works:
     * Gateway ensures only the right role categories can reach the service.
     * Bookmark service enforces ownership checks for USER role.
     *
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
        ROLE_USER > ROLE_ADMIN
        ROLE_ADMIN > ROLE_TL
        ROLE_TL > ROLE_SR
        ROLE_SR > ROLE_JR
    """);
    }


}
