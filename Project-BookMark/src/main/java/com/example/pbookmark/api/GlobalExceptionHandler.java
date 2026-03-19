package com.example.pbookmark.api;

import com.example.pbookmark.domain.errorresponse.ApiError;
import com.example.pbookmark.domain.xception.AccessDeniedException;
import com.example.pbookmark.domain.xception.BookmarkNotFoundException;
import com.example.pbookmark.domain.xception.BookmarkTitleNotAllowedException;
import com.example.pbookmark.domain.xception.DuplicateBookmarkException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(DuplicateBookmarkException.class)
    public ResponseEntity<ApiError> handleDuplicateBookmarkException(DuplicateBookmarkException e) {
        log.warn("GlobalExceptionHandler handleDuplicateBookmarkException: {}", e.getMessage());
        ApiError error = new ApiError(e.getMessage());
        error.setStatus(HttpStatus.BAD_REQUEST);
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON) // Without application/json, Feign may discard the body.
                .body(error);
    }

    @ExceptionHandler({BookmarkTitleNotAllowedException.class})
    public ResponseEntity<ApiError> handleBookmarkTitleNotAllowedException(BookmarkTitleNotAllowedException e) {
        log.warn("GlobalExceptionHandler handleBookmarkTitleNotAllowedException: {}", e.getMessage());
        ApiError error = new ApiError(e.getMessage());
        error.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        error.setTimestamp(LocalDateTime.now());
        //error.setErrors(Arrays.asList(e.getMessage()));
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON) // Without application/json, Feign may discard the body.
                .body(error);
    }

    @ExceptionHandler(BookmarkNotFoundException.class)
    public ResponseEntity<ApiError> handleBookmarkNotFoundException(BookmarkNotFoundException e) {
        log.warn("GlobalExceptionHandler handleBookmarkNotFoundException: {}", e.getMessage());
        /*ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle("Bookmark Not Found");
        //problemDetail.setType(URI.create("https://api.bookmarks.com/errors/not-found"));
        problemDetail.setType(ServletUriComponentsBuilder
                .fromCurrentRequest()
                //.path("/api/bookmarks/{id}")  // commenting this line bcoz this was giving "api/bookmarks/%7Bid%7D" in response
                .build().toUri());
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;*/
        ApiError error = new ApiError((e.getMessage()));
        error.setStatus(HttpStatus.NOT_FOUND);
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)  // Without application/json, Feign may discard the body.
                .body(error);
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Business Error : Access Denied : {}",e.getMessage());
        ApiError error = new ApiError(e.getMessage());
        error.setStatus(HttpStatus.UNAUTHORIZED);
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)  // Without application/json, Feign may discard the body.
                .body(error);
    }
}
