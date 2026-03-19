package com.learncicd.userservice.client.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class DownstreamServiceException extends RuntimeException {
    private final HttpStatus status;
    public DownstreamServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
