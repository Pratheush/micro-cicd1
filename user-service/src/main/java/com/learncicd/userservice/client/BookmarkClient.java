package com.learncicd.userservice.client;

import com.learncicd.userservice.config.FeignConfig;
import com.learncicd.userservice.exception.CustomException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Keep the Feign client clean (only API contract).
 * BUT HERE WE ARE ADDING Resilience4j Annotations for CircuitBreaker, Retry, Bulkhead, TimeLimiter. JUST FOR LEARNING
 *
 * - When you make calls with Feign clients, Spring Cloud + OpenTelemetry automatically injects tracing headers (traceparent, tracestate) into outgoing HTTP requests.
 * - This ensures that the traceId/spanId flows across service boundaries (e.g., from user-service → bookmark-service → auth-service).
 * - If you set Feign’s logger level to basic, you’ll see those headers in your dev logs. That way you can confirm that context propagation is working correctly.
 * 🔧 How to implement : In your application.properties >> spring.cloud.openfeign.client.config.default.loggerLevel=basic
 * NONE → no logging.
 * BASIC → logs request method, URL, response status, and headers (including trace headers).
 * HEADERS → logs request/response headers.
 * FULL → logs everything (headers, body, metadata).
 * NOTE ::: For dev, basic is enough to confirm trace headers are propagating. In prod, you usually keep it at none or warn to avoid log noise.
 */

@FeignClient(name="BOOKMARK-PROJECT", url ="${bookmark.service.url}",configuration = FeignConfig.class)
//@FeignClient(name="PROJECT-BOOKMARK")
public interface BookmarkClient {

    static final Logger log = LoggerFactory.getLogger(BookmarkClient.class);
    // user info should be get with login using auth-service via api-gateway


    // Delete bookmark with validation : check if bookmark exist with the title before deleting
    @DeleteMapping("/{id}")
    String delete(@PathVariable(name = "id") Long id);


    @PutMapping("/{id}")
    BookmarkDTO update(@PathVariable(name = "id") Long id,
                @RequestBody @Validated UpdateBookmarkRequest request);


    @PostMapping
    BookmarkDTO create(@RequestBody @Validated CreateBookmarkRequest request);

    /*@CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="findByIdFallback")
    @Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)*/
    @GetMapping("/{id}")
    BookmarkDTO findById(@PathVariable(name = "id") Long id);

    /**
     * In Resilience4j Annotations bind to instances, not configs settings from application.properties
     * FOR TIMELIMITER : Fails fast if a call takes too long. Requires async return type (CompletableFuture).
     * Use SEMAPHORE for synchronous Feign (correct in your code). THREADPOOL bulkhead is better for async clients.
     *
     * Why TimeLimiter Doesn’t Fit Feign
     * TimeLimiter in Resilience4j is designed for async calls (CompletableFuture, Reactor types).
     * Feign is blocking/synchronous, so TimeLimiter won’t actually cut off a slow call.
     * That’s why production setups rely on Feign’s own timeouts instead.
     *
     * We can set the connection and read timeouts that apply to every Feign Client in the application via
     * the feign.client.config.default property set in our application.yml file:
     * # max time to establish TCP connection
     * feign.client.config.default.connectTimeout=2000
     * #  max time to wait for response
     * feign.client.config.default.readTimeout=2000
     *
     * @param pageNo
     * @param pageSize
     * @return
     */
    /*@CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="findBookmarksFallback")
    @Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    //@TimeLimiter(name="USER-PROJECT-BOOKMARK-TL")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)*/
    @GetMapping
    PagedResult<BookmarkDTO> findBookmarks(
            @RequestParam(name = "page", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "size", defaultValue = "10") Integer pageSize);



    // In Fallback method : Signature must match method + Throwable last param.
    /*default PagedResult<BookmarkDTO> findBookmarksFallback(Integer pageNo,
                                              Integer pageSize,
                                              Throwable t) {
        log.warn("Bookmark service unavailable, serving cached data", t);
        //return PagedResult.empty();
        return PagedResult.withMessage("Bookmark service is temporarily unavailable. Please try again later.");
    }*/

    /*default BookmarkDTO findByIdFallback(Long id, Throwable t) {
        log.warn("BookmarkClient findByIdFallback invoked", t);
        if(t instanceof CustomException ce) {
            throw ce; // Let GlobalExceptionHandler handle it
        }
        throw new CustomException("Bookmark service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
       // return new BookmarkDTO(0L, "Bookmark Not Found Service Not Available", null, Instant.now(), null, null);
    }*/
}
