package com.learncicd.userservice.controller;

import com.learncicd.userservice.client.BookmarkDTO;
import com.learncicd.userservice.client.CreateBookmarkRequest;
import com.learncicd.userservice.client.PagedResult;
import com.learncicd.userservice.client.UpdateBookmarkRequest;
import com.learncicd.userservice.model.UserBookmarkDTO;
import com.learncicd.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/health")
    public String getUserHealth() {
        log.info("API Request: Get User Health");
        return "USER-UP-Healthy";
    }



    // Create bookmark with validation :  Validation: bookmark title must not exist before
    @PostMapping
    public ResponseEntity<BookmarkDTO> createBookmark(@RequestBody CreateBookmarkRequest request){
        BookmarkDTO bookmarkDTO = userService.createBookmark(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(bookmarkDTO);
    }

    // Update bookmark with validation : check if bookmark with the title exist
    @PutMapping("/{id}")
    public ResponseEntity<String> updateBookmark(@PathVariable Long id,
                               @RequestBody UpdateBookmarkRequest request){
        String msg = userService.updateBookmark(id, request);
        return ResponseEntity.ok(msg);
    }


    // Delete bookmark with validation : check if bookmark exist with the title before deleting
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBookmark(@PathVariable(name = "id") Long id){
        var msg = userService.deleteBookmark(id);
        return new ResponseEntity<>(msg, HttpStatus.OK);
    }

    /**
     *  Claims extraction is done via @AuthenticationPrincipal Jwt jwt in controllers.
     *  Username & email come directly from JWT claims, so user-service doesn’t need a DB.
      */
    @GetMapping("/user/bookmarks/{id}")
    public ResponseEntity<UserBookmarkDTO> getUserBookmark(@PathVariable Long id,
                                                          @AuthenticationPrincipal Jwt jwt) {
        UserBookmarkDTO userBookmarkDTO = userService.getUserBookmark(id, jwt);
        return ResponseEntity.ok(userBookmarkDTO);
    }

    @GetMapping("/user-bookmarks/{id}")
    public ResponseEntity<UserBookmarkDTO> getUserBookmark(@PathVariable Long id){
        log.info("UserController API Request: Get User Bookmark id={}", id);
        UserBookmarkDTO userbookmarkDTO = userService.getUserBookmark(id);
        log.info("API Response: UserBookmarkDTO={}", userbookmarkDTO);
        return ResponseEntity.ok(userbookmarkDTO);
    }

    @GetMapping("/user-bookmarks")
    public ResponseEntity<PagedResult<UserBookmarkDTO>> getUserAllBookmarks(
            @RequestParam(name = "page", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "size", defaultValue = "10") Integer pageSize,
            @AuthenticationPrincipal Jwt jwt){
        log.info("UserController API Request: Get User Bookmarks page={}, size={}", pageNo, pageSize);
        // PagedResult<UserBookmarkDTO> userbookmarksDTO = userService.getUserBookmarks(pageNo, pageSize, jwt);
        PagedResult<UserBookmarkDTO> userbookmarksDTO = userService.getUserBookmarksCached(pageNo, pageSize, jwt);
        log.info("UserController API Response: UserBookmarksDTO={}", userbookmarksDTO);
        return ResponseEntity.ok(userbookmarksDTO);
    }
}
