package com.example.pbookmark.domain.xception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BookmarkTitleNotAllowedException extends RuntimeException{
    public BookmarkTitleNotAllowedException(String message) {
        super(message);
    }
}
