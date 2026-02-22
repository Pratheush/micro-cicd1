package com.learncicd.apigateway;

import com.learncicd.apigateway.filter.AuthFilter;
import com.learncicd.apigateway.filter.AuthGPTFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
//@RequiredArgsConstructor
public class RoutesReactive {


    private final AuthGPTFilter authFilter;

    @Qualifier("userServiceRateLimiter")
    private final RedisRateLimiter userServiceRateLimiter;

    @Qualifier("bookmarkRateLimiter")
    private final RedisRateLimiter bookmarkRateLimiter;

    @Qualifier("ipKeyResolver")
    private final KeyResolver ipKeyResolver;

    @Qualifier("userKeyResolver")
    private final KeyResolver userKeyResolver;

    public RoutesReactive(AuthGPTFilter authFilter, RedisRateLimiter userServiceRateLimiter, RedisRateLimiter bookmarkRateLimiter, KeyResolver ipKeyResolver, KeyResolver userKeyResolver) {
        this.authFilter = authFilter;
        this.userServiceRateLimiter = userServiceRateLimiter;
        this.bookmarkRateLimiter = bookmarkRateLimiter;
        this.ipKeyResolver = ipKeyResolver;
        this.userKeyResolver = userKeyResolver;
    }

    /**
     * Eureka Load Balanced Version (Better Practice)
     * Instead of localhost URLs, use Eureka service IDs:
     * .uri("lb://TASK-SERVICE")
     * .uri("lb://USER-SERVICE")
     * .uri("lb://DISCOVERY") >> discovery-service .uri("lb://DISCOVERY-SERVICE")
     *
     * Attach filter to routes
     * .filters(f -> f.filter(authFilter.apply(new AuthFilter.Config())))
     * Even Better: Use Shortcut Name Instead of apply()
     * Spring registers your filter factory automatically using class name: AuthFilter → name = Auth
     * So you can use: .filters(f -> f.filter(authFilter))
     * But only if you override shortcut config — otherwise apply() is safest.
     *
     * SINCE WE ARE USING GlobalFilter, we don't need to attach Auth filter to each routes
     */

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {

        log.info("Routes >>> building reactive gateway routes");

        return builder.routes()

                // TASK SERVICE
                .route("task_service", r -> r
                        .path("/api/tasks/**")
                        //.filters(f -> f.filter(authFilter.apply(new AuthFilter.Config())))
                        //.filters(f -> f.filter(authFilter.apply(new AuthGPTFilter.Config())))
                        //.uri("http://localhost:9393"))
                        .uri("lb://TASK-SERVICE"))


                // USER SERVICE
                .route("user_service", r -> r
                        .path("/api/users/**")
                        //.filters(f -> f.filter(authFilter.apply(new AuthFilter.Config())))
                        //.filters(f -> f.filter(authFilter.apply(new AuthGPTFilter.Config())))
                        .filters(f -> f.circuitBreaker(c -> c.setName("USER-SERVICE-CB")
                                .setFallbackUri("forward:/api/fallback/userServiceFallback"))
                                .requestRateLimiter(rl -> rl.setRateLimiter(userServiceRateLimiter)
                                .setKeyResolver(userKeyResolver)
                                )
                        )
                        //.uri("http://localhost:9494"))
                        .uri("lb://USER-SERVICE"))

                // PROJECT-BOOKMARK
                .route("bookmark_service", r -> r
                        .path("/api/bookmarks/**")
                        //.filters(f -> f.filter(authFilter.apply(new AuthFilter.Config())))
                        //.filters(f -> f.filter(authFilter.apply(new AuthGPTFilter.Config())))
                        .filters(f -> f.circuitBreaker(c -> c.setName("PROJECT-BOOKMARK-CB")
                                        .setFallbackUri("forward:/api/fallback/bookmarkServiceFallback"))
                                .requestRateLimiter(rl -> rl.setRateLimiter(bookmarkRateLimiter)
                                .setKeyResolver(ipKeyResolver)
                                )
                        )
                        //.uri("http://localhost:3331"))
                        .uri("lb://PROJECT-BOOKMARK"))

                // AUTH-SERVICE
                .route("auth_service", r -> r
                        .path("/auth/**")
                        //.uri("http://localhost:9495"))
                        .uri("lb://AUTH-SERVICE"))

                // EUREKA UI BROADER - OPTIONAL EITHER THIS ONE OR BELOW TWO CONFIG
                /*.route("discovery_ui", r -> r
                        .path("/eureka", "/eureka/", "/eureka/web")
                        .filters(f -> f.setPath("/"))
                        .uri("http://localhost:8761"))*/

                // EUREKA UI ROOT
                .route("discovery_service", r -> r
                        .path("/eureka/web")
                        .filters(f -> f.setPath("/"))      // rewrite /eureka/web → /
                        .uri("http://localhost:8761"))
                        //.uri("lb://DISCOVERY-SERVICE"))

                // EUREKA STATIC RESOURCES
                /*.route("discovery_service_static", r -> r
                        .path("/eureka/**")
                        .uri("http://localhost:8761"))
                        //.uri("lb://DISCOVERY-SERVICE"))*/

                .build();
    }
}
