package com.learncicd.userservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.learncicd.userservice.auth.JwtUtil;
import com.learncicd.userservice.cache.config.RedisService;
import com.learncicd.userservice.client.*;
import com.learncicd.userservice.exception.CustomException;
import com.learncicd.userservice.fallbackconfig.FallbackHandler;
import com.learncicd.userservice.model.UserBookmarkDTO;
import com.learncicd.userservice.repository.UserRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Use Retries only for read operations (safe to repeat).
 * Use Bulkheads for write operations (risk of data corruption).
 * Use CircuitBreaker + Bulkhead for both reads and writes, but avoid retries on writes to prevent duplicate actions.
 * Keep fallbacks meaningful (cached data, user-friendly messages, or safe defaults).
 * // ✅ CircuitBreaker + Bulkhead for delete (no retry to avoid duplicate deletes)
 * // ✅ CircuitBreaker + Bulkhead for update (no retry to avoid duplicate updates)
 * // ✅ CircuitBreaker + Bulkhead for create (no retry to avoid duplicate creates)
 * // ✅ Reads: safe to retry
 *
 *
 * type = SEMAPHORE → Uses a semaphore to limit concurrent executions (no separate thread pool).
 * max-concurrent-calls=20 → At most 20 threads can enter this protected section at once.
 * max-wait-duration=0 → If the bulkhead is full, calls are immediately rejected (no waiting queue).
 *
 * @TimeLimiter expects the method to return an asynchronous type such as CompletionStage<T> or
 * Future<T> (e.g., CompletableFuture<T>), not a plain synchronous object.
 *
 * Why fallback is still firing
 * Resilience4j is wrapping the Feign call, so when a CustomException is thrown, it bubbles up to the AOP proxy.
 * If your fallback method exists, Resilience4j will invoke it unless the exception is explicitly ignored or
 * re-thrown inside the fallback. Adjust fallback methods to propagate business errors
 *
 * ✅ What this achieves ::: Adjust fallback methods to propagate business errors
 * 401/403/422 → re-thrown, handled by your GlobalExceptionHandler, so the client sees the original error message.
 * 5xx, timeouts, IO errors → fallback returns a safe response.
 *
 * ✅ Industry best practice pattern
 * - Decode downstream errors into domain exceptions (your CustomErrorDecoder does this).
 * - Configure Resilience4j predicates so 4xx errors don’t increment failure counts.
 * - Fallback methods re‑throw business exceptions so they propagate normally.
 * - Fallback returns safe defaults only for system failures.
 * - This is exactly the pattern recommended in production microservice architectures: business errors propagate, resilience handles outages.
 *
 *
 * final recommended design ::
 * createBookmark()        → no fallback
 * updateBookmark()        → no fallback
 * deleteBookmark()        → no fallback
 * getUserBookmark()       → no fallback
 * getUserBookmarks()      → fallback allowed
 *
 * No fallback. If bookmark service fails:
 * Feign
 *  ↓
 * CustomErrorDecoder
 *  ↓
 * CustomException / DownstreamServiceException
 *  ↓
 * GlobalExceptionHandler
 *  ↓
 * HTTP error returned
 *
 * Rule used in production microservices::
 * WRITE operations → never fallback. Because: Write = must be correct
 * READ operations → fallback allowed. Because : Read = can be degraded
 *
 * Typical production systems:
 * CREATE → error
 * UPDATE → error
 * DELETE → error
 * GET BY ID → error
 * LIST / SEARCH → fallback allowed
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;

    private final BookmarkClient bookmarkClient;

    private final RedisService redisService;

    private final FallbackHandler fallbackHandler;


    /**
     * a fast-fail bulkhead: if more than 20 concurrent calls hit USER-PROJECT-BOOKMARK-BH,
     * the 21st call is rejected immediately. This is good for protecting a fragile service,
     * but if you want smoother handling under load, you could allow a small wait duration (e.g., max-wait-duration=100ms).
     * @param id
     * @return
     */
    // Delete bookmark with validation : check if bookmark exist with the title before deleting
    // ✅ CircuitBreaker + Bulkhead for delete (no retry to avoid duplicate deletes)
    //@CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="deleteBookmarkFallback")
    @CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB")
    //@Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)
    public String deleteBookmark(Long id){
        log.info("UserService deleteBookmark API Request: Delete Bookmark id={}", id);
        return bookmarkClient.delete(id);
    }

    // Update bookmark with validation : check if bookmark with the title exist
    // ✅ CircuitBreaker + Bulkhead for update (no retry to avoid duplicate updates)
    //@CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="updateBookmarkFallback")
    @CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB")
    //@Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)
    public BookmarkDTO updateBookmark(Long id, UpdateBookmarkRequest request){
        log.info("UserService updateBookmark API Request: Update Bookmark id={}, UpdateBookmarkRequest={}", id, request);
        return bookmarkClient.update(id, request);
    }

    // Create bookmark with validation :  Validation: bookmark title must not exist before
    // ✅ CircuitBreaker + Bulkhead for create (no retry to avoid duplicate creates)
    //@CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="createBookmarkFallback")
    @CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB")
    //@Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)
    public BookmarkDTO createBookmark(CreateBookmarkRequest request){
        log.info("UserService createBookmark API Request: CreateBookmarkRequest={}", request);
        return bookmarkClient.create(request);
    }

    public UserBookmarkDTO getUserBookmark(Long id) {
        log.info("UserService getUserBookmark without Jwt API Request: Get User Bookmark id={}", id);
        BookmarkDTO bookmarkDTO = bookmarkClient.findById(id);
        log.info("API Response: Bookmark DTO={}", bookmarkDTO);
        return new UserBookmarkDTO(null,null, bookmarkDTO.id(), bookmarkDTO.title(),bookmarkDTO.url(),bookmarkDTO.createdAt(),bookmarkDTO.createdBy(),bookmarkDTO.updatedBy());
    }


    public PagedResult<UserBookmarkDTO> getUserBookmarks(Integer pageNo, Integer pageSize, Jwt jwt) {

        //PagedResult<UserBookmarkDTO> pagedUserBookmarksDTO = redisService.get("user-bookmarks",PagedResult<UserBookmarkDTO>.class);
        String username = jwt.getSubject(); // from setSubject()
        String email = jwt.getClaim("email"); // custom claim

        Claims claims = jwtUtil.getClaims(jwt.getTokenValue());
        String claimEmail = claims.get("email", String.class);
        String roles = claims.get("roles", String.class);
        log.info("UserService getUserBookmarksUser Info: username={}, email={}, claimEmail={}, roles={}", username, email, claimEmail, roles);

        log.info("UserService API Request: Get User Bookmarks page={}, size={}", pageNo, pageSize);
        PagedResult<BookmarkDTO> pagedBookmarksDTO = bookmarkClient.findBookmarks(pageNo, pageSize);
        log.info("UserService API Response: Bookmarks DTO={}", pagedBookmarksDTO);
        List<BookmarkDTO> bookmarkDTOList = pagedBookmarksDTO.data();
        List<UserBookmarkDTO> userBookmarkDTOList = bookmarkDTOList.stream()
                .map(bookmarkDTO -> new UserBookmarkDTO(username, email, bookmarkDTO.id(), bookmarkDTO.title(), bookmarkDTO.url(),bookmarkDTO.createdAt(),bookmarkDTO.createdBy(),bookmarkDTO.updatedBy()))
                .toList();

        PagedResult<UserBookmarkDTO> pagedUserBookmarksDTO = new PagedResult<>(userBookmarkDTOList, pagedBookmarksDTO.totalElements(), pagedBookmarksDTO.pageNumber(), pagedBookmarksDTO.totalPages(), pagedBookmarksDTO.isFirst(), pagedBookmarksDTO.isLast(), pagedBookmarksDTO.hasNext(), pagedBookmarksDTO.hasPrevious(), Optional.empty());

        //if(pagedUserBookmarksDTO.data().size() > 0) redisService.set("user-bookmarks",pagedUserBookmarksDTO, 2L);
        log.info("UserService API Response: UserBookmarksDTO={}", pagedUserBookmarksDTO);
        return pagedUserBookmarksDTO;
    }


    @CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="getUserBookmarksCachedFallback")
    @Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    //@TimeLimiter(name="USER-PROJECT-BOOKMARK-TL")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)
    public PagedResult<UserBookmarkDTO> getUserBookmarksCached(Integer pageNo, Integer pageSize, Jwt jwt) {
        log.info("UserService getUserBookmarksCached pageNumber : {}, pageSize : {}",pageNo,pageSize);
        // Build a user-specific cache key
        String username = jwt.getSubject();
        String email = jwt.getClaim("email");
        String cacheKey = String.format("user:%s:%s:bookmarks:page:%d:size:%d", username, email, pageNo, pageSize);

        // Try cache first
        PagedResult<UserBookmarkDTO> cached = redisService.get(cacheKey, new TypeReference<PagedResult<UserBookmarkDTO>>() {});
        if (cached != null) {
            log.info("Redis CACHE HIT key={}", cacheKey);
            return cached;
        }

        log.info("Redis CACHE MISS key={}", cacheKey);

        // Extract claims for logging/debugging
        /**
         * Fetch JWT claims (optional — already partly available from Jwt)
         */
        Claims claims = jwtUtil.getClaims(jwt.getTokenValue());
        String claimEmail = claims.get("email", String.class);
        String roles = claims.get("roles", String.class);
        log.info("UserService getUserBookmarks user info: username={}, email={}, claimEmail={}, roles={}",
                username, email, claimEmail, roles);

        // Call downstream service
        log.info("UserService API Request: GetUserBookmarksCached page={}, size={}", pageNo, pageSize);
        PagedResult<BookmarkDTO> pagedBookmarksDTO = bookmarkClient.findBookmarks(pageNo, pageSize);

        // Transform into user-specific DTOs List
        /**
         * Map BookmarkDTO → UserBookmarkDTO
         */
        List<UserBookmarkDTO> userBookmarkDTOList = pagedBookmarksDTO.data().stream()
                .map(b -> new UserBookmarkDTO(username, email, b.id(), b.title(), b.url(),b.createdAt(),b.createdBy(),b.updatedBy()))
                .toList();

        PagedResult<UserBookmarkDTO> result = new PagedResult<>(
                userBookmarkDTOList,
                pagedBookmarksDTO.totalElements(),
                pagedBookmarksDTO.pageNumber(),
                pagedBookmarksDTO.totalPages(),
                pagedBookmarksDTO.isFirst(),
                pagedBookmarksDTO.isLast(),
                pagedBookmarksDTO.hasNext(),
                pagedBookmarksDTO.hasPrevious(),
                Optional.empty()
        );

        // Cache the result with short TTL
        /**
         * Store in Redis only if not empty
         */
        if (!result.data().isEmpty()) {
            redisService.set(cacheKey, result, 1L); // TTL = 1 minutes
            log.info("Cached result for {}", cacheKey);
        }

        log.info("UserService API Response: GetUserBookmarksCached : UserBookmarksDTO Size={}", result.data().size());
        return result;
    }



    // user info should be get with login using auth-service via api-gateway
    // Fetch bookmark with user info : getUserBookmark : validation if bookmark exist or not
    // BookmarkDTO findById(@PathVariable(name = "id") Long id);
    //@CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="getUserBookmarkFallback")
    @CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB")
    @Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)
    public UserBookmarkDTO getUserBookmark(Long bookmarkId, Jwt jwt){
        log.info("UserService getUserBookmark with Jwt API Request: Get User Bookmark id={}", bookmarkId);
        String username = jwt.getSubject(); // from setSubject()
        String email = jwt.getClaim("email"); // custom claim
        List<String> authoritiesList = jwt.getClaimAsStringList("authorities");

        Claims claims = jwtUtil.getClaims(jwt.getTokenValue());
        String claimEmail = claims.get("email", String.class);
        String roles = claims.get("roles", String.class);
        log.info("UserService getUserBookmark User Info: username={}, email={}, claimEmail={}, roles={}, authorities={}", username, email, claimEmail, roles,authoritiesList);

        BookmarkDTO bookmarkDTO = bookmarkClient.findById(bookmarkId);
        log.debug("UserService getUserBookmark : BookmarkDTO Response : {}",bookmarkDTO);
        return new UserBookmarkDTO(username,email,bookmarkDTO.id(),bookmarkDTO.title(),bookmarkDTO.url(),bookmarkDTO.createdAt(),bookmarkDTO.createdBy(),bookmarkDTO.updatedBy());
    }

    /**
     * Adjust fallback methods to propagate business errors
     * @param id
     * @param ex
     * @return
     */
    /*public String deleteBookmarkFallback(Long id, Throwable ex) {
        log.info("Fallback triggered for deleteBookmarkFallback {}", ex.getMessage());
        if (ex instanceof CustomException ce && ce.getStatus().is4xxClientError()) {
            throw ce; // propagate business errors
        }
        //throw new RuntimeException("Bookmark service temporarily unavailable");
        // Provide degraded response only for system failures
        return "DELETE OPERATION FAILED Bookmark service unavailable,Try again later";
    }*/

    /**
     * Adjust fallback methods to propagate business errors
     * @param id
     * @param request
     * @param t
     * @return
     */
    /*public BookmarkDTO updateBookmarkFallback(Long id, UpdateBookmarkRequest request, Throwable t) {
        log.info("UserService : updateBookmarkFallback invoked UpdateBookmarkRequest {}",request);
        if (t instanceof CustomException ce && ce.getStatus().is4xxClientError()) {
            // Propagate business error instead of returning fallback
            throw ce;
        }

        // Provide degraded response only for system failures
        return new BookmarkDTO(0L, "Bookmark Not Updated Service Not Available", null, Instant.now(), null, null);
    }*/

    /**
     * Adjust fallback methods to propagate business errors
     * @param request
     * @param t
     * @return
     */
    /*public BookmarkDTO createBookmarkFallback(CreateBookmarkRequest request, Throwable t) {
        log.info("UserService : createBookmarkFallback invoked CreateBookmarkRequest {}",request);
        if (t instanceof CustomException ce && ce.getStatus().is4xxClientError()) {
            // Propagate business error instead of returning fallback
            throw ce;
        }
        // Provide degraded response only for system failures
        return new BookmarkDTO(0L, "Bookmark Not Created Service Not Available", null, Instant.now(),null,null);
    }*/

    /**
     * Adjust fallback methods to propagate business errors
     * @param pageNo
     * @param pageSize
     * @param jwt
     * @param t
     * @return
     */
    /*public PagedResult<UserBookmarkDTO> getUserBookmarksCachedFallback(Integer pageNo, Integer pageSize, Jwt jwt,Throwable t) {
        //return PagedResult.empty();
        if (t instanceof CustomException ce && ce.getStatus().is4xxClientError()) {
            // Propagate business error instead of returning fallback
            throw ce;
        }
        log.warn("Bookmark service unavailable : {}", t.getMessage());
        // Provide degraded response only for system failures
        return PagedResult.withMessage("BOOKMARKS UNAVAILABLE Bookmark service is temporarily unavailable. Please try again later.");
    }*/

    /*public UserBookmarkDTO getUserBookmarkFallback(Long bookmarkId, Jwt jwt,Throwable t) {
        log.warn("BookmarkClient findByIdFallback invoked", t);
        if(t instanceof CustomException ce) {
            throw ce; // Let GlobalExceptionHandler handle it
        }
        throw new CustomException("Bookmark service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        // return new BookmarkDTO(0L, "Bookmark Not Found Service Not Available", null, Instant.now(), null, null);
    }*/

    /**
     * Adjust fallback methods to propagate business errors
     * @param bookmarkId
     * @param jwt
     * @param ex
     * @return
     */
    /*public UserBookmarkDTO getUserBookmarkFallback(Long bookmarkId,Jwt jwt, Throwable ex) {
        if (ex instanceof CustomException ce && ce.getStatus().is4xxClientError()) {
            throw ce; // propagate business errors
        }
        log.warn("Fallback triggered for bookmarkId={}, reason={}", bookmarkId, ex.getMessage());

        // Provide degraded response only for system failures
        return new UserBookmarkDTO(null,null,0L, "Bookmark Not Found Service Not Available", null, Instant.now(), null, null);
    }*/



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

    /*public String deleteBookmarkFallback(Long id, Throwable ex) {
        return fallbackHandler.handle(ex,
                () -> "DELETE OPERATION FAILED Bookmark service unavailable,Try again later");
    }*/

    /*public BookmarkDTO updateBookmarkFallback(Long id, UpdateBookmarkRequest request, Throwable t) {
        return fallbackHandler.handle(t,
                () -> new BookmarkDTO(0L, "Bookmark Not Updated Service Not Available",
                        null, Instant.now(), null, null));
    }*/

    /*public BookmarkDTO createBookmarkFallback(CreateBookmarkRequest request, Throwable t) {
        return fallbackHandler.handle(t,
                () -> new BookmarkDTO(0L, "Bookmark Not Created Service Not Available", null, Instant.now(),null,null));
    }*/

    public PagedResult<UserBookmarkDTO> getUserBookmarksCachedFallback(Integer pageNo, Integer pageSize, Jwt jwt,Throwable t) {
       return fallbackHandler.handle(t,
               () -> PagedResult.withMessage("BOOKMARKS UNAVAILABLE Bookmark service is temporarily unavailable. Please try again later."));
    }

    /*public UserBookmarkDTO getUserBookmarkFallback(Long bookmarkId,Jwt jwt, Throwable ex) {
        return fallbackHandler.handle(ex,
                () -> new UserBookmarkDTO(null,null,0L, "Bookmark Not Found Service Not Available", null, Instant.now(), null, null));
    }*/
}

