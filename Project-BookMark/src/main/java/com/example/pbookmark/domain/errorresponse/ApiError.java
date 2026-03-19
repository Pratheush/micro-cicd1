package com.example.pbookmark.domain.errorresponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {

    private HttpStatus status;
    private LocalDateTime timestamp;
    private String message;
    //private List<String> errors;

    public ApiError(String message) {
        this.message=message;
    }
}
