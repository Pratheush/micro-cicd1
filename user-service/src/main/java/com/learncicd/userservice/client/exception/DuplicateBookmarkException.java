package com.learncicd.userservice.client.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DuplicateBookmarkException extends RuntimeException{
    public DuplicateBookmarkException(String message) {
        super(message);
    }
}
