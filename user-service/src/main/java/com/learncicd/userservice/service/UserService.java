package com.learncicd.userservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.learncicd.userservice.auth.JwtUtil;
import com.learncicd.userservice.cache.config.RedisService;
import com.learncicd.userservice.client.*;
import com.learncicd.userservice.model.UserBookmarkDTO;
import com.learncicd.userservice.repository.UserRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;

    private final BookmarkClient bookmarkClient;

    private final RedisService redisService;



    // Delete bookmark with validation : check if bookmark exist with the title before deleting
    // ✅ CircuitBreaker + Bulkhead for delete (no retry to avoid duplicate deletes)
    @CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="deleteBookmarkFallback")
    //@Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)
    public String deleteBookmark(Long id){
        bookmarkClient.delete(id);
        return "Bookmark deleted successfully";
    }

    // Update bookmark with validation : check if bookmark with the title exist
    // ✅ CircuitBreaker + Bulkhead for update (no retry to avoid duplicate updates)
    @CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="updateBookmarkFallback")
    //@Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)
    public String updateBookmark(Long id, UpdateBookmarkRequest request){
        bookmarkClient.update(id, request);
        return "Bookmark updated successfully";
    }

    // Create bookmark with validation :  Validation: bookmark title must not exist before
    // ✅ CircuitBreaker + Bulkhead for create (no retry to avoid duplicate creates)
    @CircuitBreaker(name="USER-PROJECT-BOOKMARK-CB", fallbackMethod="createBookmarkFallback")
    //@Retry(name="USER-PROJECT-BOOKMARK-RETRY")
    @Bulkhead(name="USER-PROJECT-BOOKMARK-BH", type = Bulkhead.Type.SEMAPHORE)
    public BookmarkDTO createBookmark(CreateBookmarkRequest request){
        return bookmarkClient.create(request);
    }

    public UserBookmarkDTO getUserBookmark(Long id) {
        log.info("UserServiceAPI Request: Get User Bookmark id={}", id);
        BookmarkDTO bookmarkDTO = bookmarkClient.findById(id);
        log.info("API Response: Bookmark DTO={}", bookmarkDTO);
        return new UserBookmarkDTO(null,null, bookmarkDTO.id(), bookmarkDTO.title(),bookmarkDTO.url());
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
                .map(bookmarkDTO -> new UserBookmarkDTO(username, email, bookmarkDTO.id(), bookmarkDTO.title(), bookmarkDTO.url()))
                .toList();

        PagedResult<UserBookmarkDTO> pagedUserBookmarksDTO = new PagedResult<>(userBookmarkDTOList, pagedBookmarksDTO.totalElements(), pagedBookmarksDTO.pageNumber(), pagedBookmarksDTO.totalPages(), pagedBookmarksDTO.isFirst(), pagedBookmarksDTO.isLast(), pagedBookmarksDTO.hasNext(), pagedBookmarksDTO.hasPrevious(), Optional.empty());

        //if(pagedUserBookmarksDTO.data().size() > 0) redisService.set("user-bookmarks",pagedUserBookmarksDTO, 2L);
        log.info("UserService API Response: UserBookmarksDTO={}", pagedUserBookmarksDTO);
        return pagedUserBookmarksDTO;
    }


    public PagedResult<UserBookmarkDTO> getUserBookmarksCached(Integer pageNo, Integer pageSize, Jwt jwt) {
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
                .map(b -> new UserBookmarkDTO(username, email, b.id(), b.title(), b.url()))
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

        log.info("UserService API Response: GetUserBookmarksCached : UserBookmarksDTO={}", result);
        return result;
    }



    // user info should be get with login using auth-service via api-gateway
    // Fetch bookmark with user info : getUserBookmark : validation if bookmark exist or not
    // BookmarkDTO findById(@PathVariable(name = "id") Long id);
    public UserBookmarkDTO getUserBookmark(Long bookmarkId, Jwt jwt){
        String username = jwt.getSubject(); // from setSubject()
        String email = jwt.getClaim("email"); // custom claim

        Claims claims = jwtUtil.getClaims(jwt.getTokenValue());
        String claimEmail = claims.get("email", String.class);
        String roles = claims.get("roles", String.class);
        log.info("UserService getUserBookmark User Info: username={}, email={}, claimEmail={}, roles={}", username, email, claimEmail, roles);

        BookmarkDTO bookmarkDTO = bookmarkClient.findById(bookmarkId);
        return new UserBookmarkDTO(username,email,bookmarkDTO.id(),bookmarkDTO.title(),bookmarkDTO.url());
    }

    public String deleteBookmarkFallback(Long id, Throwable t) {
        log.warn("Bookmark service unavailable, serving cached data", t);
        return "Bookmark service unavailable,Try again later";
    }

    public String updateBookmarkFallback(Long id, UpdateBookmarkRequest request, Throwable t) {
        log.warn("Bookmark service unavailable, serving cached data", t);
        return "Bookmark service unavailable,Try again later";
    }

    public BookmarkDTO createBookmarkFallback(CreateBookmarkRequest request, Throwable t) {
        log.warn("Bookmark service unavailable, serving cached data", t);
        return new BookmarkDTO(0L, "Bookmark Not Created Service Not Available", null, Instant.now());
    }
}

