package com.learncicd.apigateway;

//import lombok.NonNull;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
//import org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions;
//import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
//import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.reactive.function.server.HandlerFunction;
//
//import org.springframework.web.servlet.function.RequestPredicates;
//import org.springframework.web.servlet.function.RouterFunction;
//import org.springframework.web.servlet.function.ServerResponse;
//
//
//
//@Configuration(proxyBeanMethods = false)
//@Slf4j
//public class Routes {
//
//    @Bean
//    public RouterFunction<@NonNull ServerResponse> taskServiceRoute() {
//        log.info("Routes >>> taskServiceRoute");
//        return GatewayRouterFunctions.route("task_service")
//                .route(RequestPredicates.path("/api/tasks/**"), HandlerFunctions.http())
//                .before(BeforeFilterFunctions.uri("http://localhost:9393"))
//                .build();
//    }
//
//    @Bean
//    public RouterFunction<@NonNull ServerResponse> userServiceRoute() {
//        log.info("Routes >>> userServiceRoute");
//        return GatewayRouterFunctions.route("user_service")
//                .route(RequestPredicates.path("/api/users/**"),HandlerFunctions.http())
//                .before(BeforeFilterFunctions.uri("http://localhost:9494"))
//                .build();
//    }
//
//    // in api-gateway we don't set eureka service config routes
//    // use this uri : http://localhost:8761 or use lb://DISCOVERY : here DISCOVERY is service name that is shown on eureka-dashboard
//    @Bean
//    public RouterFunction<@NonNull ServerResponse> discoveryServiceRoute() {
//        log.info("Routes >>> discoveryServiceRoute");
//        return GatewayRouterFunctions.route("discovery_service")
//                .route(RequestPredicates.path("/eureka/web"),HandlerFunctions.http())
//                .before(BeforeFilterFunctions.uri("http://localhost:8761"))
//                .filter(FilterFunctions.setPath("/"))
//                .build();
//    }
//
//    @Bean
//    public RouterFunction<@NonNull ServerResponse> discoveryServiceStaticRoute() {
//        log.info("Routes >>> discoveryServiceStaticRoute");
//        return GatewayRouterFunctions.route("discovery_service_static")
//                .route(RequestPredicates.path("/eureka/**"),HandlerFunctions.http())
//                .before(BeforeFilterFunctions.uri("http://localhost:8761"))
//                .build();
//    }
//
//}
