package com.learncicd.authservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * All three are client-side/business errors (4xx) → The client sent invalid input, requested a missing resource, or tried to register an existing username.
 * These are not server failures. They should be logged at WARN, not ERROR.
 * INFO would be too low — you’d miss important misuse patterns.
 * ERROR would be misleading — it would inflate error dashboards with expected client mistakes.
 * 👉 Reserve log.error for unexpected server-side failures (e.g., DB down, NullPointerException).
 * 👉 Use log.info for normal flows (successful login, token issued, etc.).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Business Error Resource Not Found : {}",ex.getMessage());
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getStatus());
        return ResponseEntity.status(ex.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        log.warn("Business Error Bad Request : {}",ex.getMessage());
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getStatus());
        return ResponseEntity.status(ex.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(UsernameAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExistException(UsernameAlreadyExistException ex) {
        log.warn("Business Error : {}",ex.getMsg());
        ErrorResponse response = new ErrorResponse(ex.getMsg(), ex.getStatus());
        return ResponseEntity.status(ex.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
