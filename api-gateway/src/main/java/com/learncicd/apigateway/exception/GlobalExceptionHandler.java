package com.learncicd.apigateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * - INFO → Use when the exception is part of a normal business flow and not really a problem. Example: validation fails and you expect clients to sometimes send bad data.
 * - WARN → Use when the exception indicates a client-side issue or something unusual, but not a server failure. Example: a malformed request, invalid parameters, or unauthorized access attempts.
 * - ERROR → Use when the exception indicates a server-side failure or something that requires investigation. Example: database down, null pointer, unexpected runtime error.
 *  🎯 Best practice
 * Keep BadRequestException at WARN.
 * Use ERROR only for unexpected server-side failures.
 * Use INFO for successful flows or benign events.
 */

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        log.warn("Business Error : {}",ex.getMessage());
        ErrorResponse response = new ErrorResponse(ex.getMessage(), ex.getStatus());
        return ResponseEntity
                .status(ex.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

}
