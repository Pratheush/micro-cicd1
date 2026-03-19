package com.example.pbookmark.domain.xception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED,code = HttpStatus.UNAUTHORIZED,reason = "UNAUTHORIZED Operation Is Not Allowed")
public class AccessDeniedException extends RuntimeException{

    public AccessDeniedException(String message) {
        super(message);
    }
}
