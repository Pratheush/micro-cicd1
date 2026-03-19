package com.learncicd.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/bookmarkServiceFallback")
    public Mono<String> bookmarkServiceFallback() {
        log.error("Bookmark Service is down. Please try again later.");
        return Mono.just("Bookmark Service is down. Please try again later.");
    }

    @GetMapping("/userServiceFallback")
    public Mono<String> userServiceFallback() {
        log.error("User Service is down. Please try again later.");
        return Mono.just("User Service is down. Please try again later.");
    }
}
