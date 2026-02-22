package com.learncicd.apigateway.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BadRequestException extends RuntimeException{

    private String message;
    private HttpStatus status;

    public BadRequestException(String message, HttpStatus status) {
        super(message);
        this.message = message;
        this.status = status;
    }
}
