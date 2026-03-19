package com.learncicd.userservice.config;

import com.learncicd.userservice.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.function.Predicate;
/**
 * When I uncomment the Resilience4j annotations (@CircuitBreaker, @Retry, @Bulkhead), any Feign exception from Project-Bookmark is
 * being treated as a failure by Resilience4j, so the fallback method is triggered automatically. That means even business
 * errors (like 401 Unauthorized or 422 Unprocessable Entity) are being routed to your fallback instead of propagating the original error message.
 *
 * This happens because by default, Resilience4j considers all exceptions as failures unless you configure a recordFailurePredicate or ignoreExceptions.
 *
 * After writing RecordFailurePredicate register into your config by wiring it into Bean or application.properties.
 * so Resilience4j knows not to treat CustomException (with 4xx statuses) as failures.
 *
 * 4xx errors (401, 403, 422) → Not counted as failures, no fallback triggered. They propagate through your CustomErrorDecoder + GlobalExceptionHandler.
 * 5xx errors, timeouts, IO issues → Counted as failures, CircuitBreaker trips, fallback methods are invoked.
 *
 * Without this, Resilience4j defaults to “all exceptions = failure.”
 */
@Slf4j
public class RecordFailurePredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
    log.info("RecordFailurePredicate test");
        Throwable root = unwrap(throwable);

        if (root instanceof CustomException custom) {

            // Since 401/403 should NOT trigger fallback
            if (custom.getStatus() == HttpStatus.UNAUTHORIZED ||
                    custom.getStatus() == HttpStatus.FORBIDDEN) {
                log.warn("RecordFailurePredicate test FORBIDDEN UNAUTHORIZED ");
                return false; // NOT a failure
            }

            // DO NOT record 4xx errors
            if (custom.getStatus().is4xxClientError()) {
                log.warn("RecordFailurePredicate test is4xxClientError ");
                return false;
            }

            // Record 5xx errors
            if (custom.getStatus().is5xxServerError()) {
                log.error("RecordFailurePredicate test is5xxServerError ");
                return true;
            }
        }

        // Record all other exceptions (timeouts, IO, etc)
        return true;
    }

    private Throwable unwrap(Throwable throwable) {
        log.info("RecordFailurePredicate unwrap ");
        while (throwable.getCause() != null) {
            log.info("RecordFailurePredicate unwrap while block");
            throwable = throwable.getCause();
        }
        return throwable;
    }
}