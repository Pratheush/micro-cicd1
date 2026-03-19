package com.learncicd.authservice.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class UsernameAlreadyExistException extends RuntimeException{
    private String msg;
    private HttpStatus status;
    public UsernameAlreadyExistException(String msg){
        this.msg=msg;
        this.status=HttpStatus.BAD_REQUEST;
    }
}
