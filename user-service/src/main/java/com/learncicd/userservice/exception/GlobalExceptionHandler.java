package com.learncicd.userservice.exception;

import com.learncicd.userservice.client.exception.AccessDeniedException;
import com.learncicd.userservice.client.exception.BookmarkNotFoundException;
import com.learncicd.userservice.client.exception.DownstreamServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * - Business errors (invalid input, unauthorized, forbidden) are part of normal flow → WARN, no stack trace.
 * - System failures (timeouts, IO errors, DB issues) are abnormal → ERROR, full stack trace for debugging.
 * - Unexpected exceptions → ERROR, full stack trace so you don’t miss hidden bugs.
 * - Granularity → You can add log.debug for extra context in dev, but keep WARN/ERROR clean in prod.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Business errors (401, 403, 422) → WARN without stack trace
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        //log.warn("User-Service GlobalExceptionHandler handleCustomException ex:{}",ex.getMessage());
        log.warn("Business error: {}", ex.getMessage()); // no stack trace
        ErrorResponse error = new ErrorResponse(ex.getMessage());
        error.setStatus(ex.getStatus());
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity
                .status(ex.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    // System failures (timeouts, IO errors) → ERROR with stack trace
    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ErrorResponse> handleDownstreamServiceException(DownstreamServiceException ex) {
        //log.warn("User-Service GlobalExceptionHandler handleCustomException ex:{}",ex.getMessage());
        log.error("System failure occurred: {}", ex.getMessage(),ex); // includes stack trace
        ErrorResponse error = new ErrorResponse(ex.getMessage());
        error.setStatus(ex.getStatus());
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity
                .status(ex.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Business error AccessDeniedException : {}", e.getMessage()); // no stack trace
        ErrorResponse error = new ErrorResponse(e.getMessage());
        error.setStatus(HttpStatus.UNAUTHORIZED);
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    // Catch-all for unexpected errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex); // full stack trace
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error");
    }

}
