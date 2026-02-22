package com.learncicd.taskservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ResourceNotFoundException extends RuntimeException{
    private String message;
    private HttpStatus status;

    public ResourceNotFoundException(String message) {
        super(message);
        this.message=message;
        this.status=HttpStatus.NOT_FOUND;
    }
}
