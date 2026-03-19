package com.learncicd.apigateway.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * This logs user + roles + endpoint.
 * WHY DEBUG : Because this is verbose per-request logging.
 *
 * What it does:
 * 1. Runs as a WebFlux filter for every request.
 * 2. Extracts the Principal (the authenticated user).
 * 3. Casts it to Authentication so you can read:
 *    - auth.getName() → username.
 *    - auth.getAuthorities() → roles/authorities.
 * 4. Logs an audit entry for every access attempt:
 *    - Who (user)
 *    - What roles they have
 *    - Which HTTP method
 *    - Which path
 * 👉 This gives us a trace of every request attempt, useful for auditing and debugging.
 *
 * 1. Log level configuration
 *      - In your SecurityAuditWebFilter, you’re using log.debug(...).
 *      - If your logging configuration (e.g., application.yml or logback.xml) is set to INFO or higher, debug logs won’t appear.
 *      - Similarly, authenticationEntryPoint and accessDeniedHandler use log.warn(...), which should show up unless your root logger is set to ERROR.
 *
 * 2. Log Level Configuration in Spring Boot
 * SpringBoot uses application.yml (or application.properties) to control logging levels.
 * By default, the root logger is set to INFO. That means:
 *  - log.debug(...) → not shown (too low).
 *  - log.info(...) → shown.
 *  - log.warn(...) → shown.
 *  - log.error(...) → shown.
 *
 * So in your SecurityAuditWebFilter, you used log.debug(...). Unless you explicitly set the level to DEBUG, those lines will be suppressed.
 */
@Component
@Slf4j
public class SecurityAuditWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        return exchange.getPrincipal()
                .cast(Authentication.class)
                .doOnNext(auth -> {

                    String username = auth.getName();
                    String roles = auth.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));

                    log.info("INFO ACCESS_ATTEMPT user={} roles={} method={} path={}",
                            username,
                            roles,
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getURI().getPath());

                    log.debug("DEBUG ACCESS_ATTEMPT user={} roles={} method={} path={}",
                            username,
                            roles,
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getURI().getPath());
                })
                .doOnError(err -> log.error("Principal error", err)) // Add .doOnSubscribe(...) or .doOnError(...) to log even when no principal is present.
                .switchIfEmpty(Mono.fromRunnable(() ->
                {
                    log.info("INFO ACCESS_ATTEMPT anonymous method={} path={}",
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getURI().getPath());

                    log.debug("DEBUG ACCESS_ATTEMPT anonymous method={} path={}",
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getURI().getPath());
                }
                ))
                .then(chain.filter(exchange));
    }
}
