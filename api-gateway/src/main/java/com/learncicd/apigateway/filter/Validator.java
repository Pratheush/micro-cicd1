package com.learncicd.apigateway.filter;

//import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.function.Predicate;

/**
 * ✅ How To Whitelist Public Endpoints
 */
@Component
public class Validator {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public static  final List<String> openApiEndpoints  = List.of(
            "/auth/register-user",
            "/auth/generate-token",
            //"/validate-token/{token}",
            "/eureka/web",
            "/eureka/**",
            "/actuator/health"
    );

    public Predicate<ServerHttpRequest> predicate = serverHttpRequest -> {
        String requestPath = serverHttpRequest.getURI().getPath();
        return openApiEndpoints .stream()
                .noneMatch(uri -> antPathMatcher.match(uri, requestPath));
    };

    public Predicate<ServerHttpRequest> predicate1 = request ->
            openApiEndpoints.stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}
