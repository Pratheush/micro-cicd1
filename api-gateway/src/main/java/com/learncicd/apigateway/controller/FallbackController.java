package com.learncicd.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/fallback")
public class FallbackController {

    @GetMapping("/bookmarkServiceFallback")
    public Mono<String> bookmarkServiceFallback() {
        return Mono.just("Bookmark Service is down. Please try again later.");
    }

    @GetMapping("/userServiceFallback")
    public Mono<String> userServiceFallback() {
        return Mono.just("User Service is down. Please try again later.");
    }
}
