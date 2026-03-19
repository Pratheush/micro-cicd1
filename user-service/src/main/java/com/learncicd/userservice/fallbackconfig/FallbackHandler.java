package com.learncicd.userservice.fallbackconfig;

import com.learncicd.userservice.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Problem with my EARLIER design ::
 * UserService contains multiple fallback methods:
 * - deleteBookmarkFallback
 * - updateBookmarkFallback
 * - createBookmarkFallback
 * - getUserBookmarksCachedFallback
 * - getUserBookmarkFallback
 *
 * Issues:
 * • business logic mixed with fallback logic
 * • duplicated error checks
 * • duplicated logging
 * • harder to maintain
 *
 * Cleaner architecture used in production :: Instead of multiple fallback methods, create one reusable fallback handler component. all fallbacks go through one handler.
 * service
 *  └── UserService
 *
 * resilience
 *  └── FallbackHandler   ← centralized
 *
 * feign
 *  └── BookmarkClient
 *
 * Advantages:
 * • consistent logging
 * • no duplicate code
 * • easier testing
 * • easier maintenance
 *
 * Business error → throw
 * Infrastructure failure → fallback
 * Centralizing that rule in one handler is the clean microservice design pattern.
 *
 * 🔎 Why signatures must match
 * Resilience4j uses reflection to call the fallback.
 * If your method is public BookmarkDTO updateBookmark(Long id, UpdateBookmarkRequest req),
 * then the fallback must be public BookmarkDTO updateBookmarkFallback(Long id, UpdateBookmarkRequest req, Throwable t). Thats non-negotiable.
 *
 * ✅ How to centralize despite signature rules:
 *  don’t eliminate the fallback methods entirely — just thin them down so they just delegate to a single reusable handler:
 *
 *  THUS :
 *  The signatures are preserved (so Resilience4j is happy).
 * The logic (business error vs infra error, logging, etc.) is centralized in FallbackHandler.
 * Each fallback method becomes a one‑liner delegating to the handler.
 *
 * 🎯 What you gain
 * Service class stays clean → only minimal fallback stubs.
 * Consistency → all error handling rules live in one place.
 * Maintainability → if you change logging or error propagation rules, you do it once.
 * Future‑proof → later you can move even further (Feign ErrorDecoder + global exception handler) to eliminate most fallbacks.
 *
 *SIMPLE RULE :
 * Fallback methods must exist (matching signatures).
 * Fallback logic doesn’t have to live there — delegate to a centralized handler.
 */
@Component
@Slf4j
public class FallbackHandler {

    public <T> T handle(Throwable ex, Supplier<T> degradedResponse) {

        if (ex instanceof CustomException ce && ce.getStatus().is4xxClientError()) {
            log.warn("FallbackHandler : handler : propagating business error");
            throw ce; // propagate business errors
        }

        log.error("External service failure: {}", ex.getMessage());

        return degradedResponse.get();
    }
}