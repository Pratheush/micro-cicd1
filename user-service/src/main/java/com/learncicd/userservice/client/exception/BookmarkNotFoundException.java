package com.learncicd.userservice.client.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND,code = HttpStatus.NOT_FOUND,reason = "Bookmark Not Found With the given id")
public class BookmarkNotFoundException extends RuntimeException {
    private BookmarkNotFoundException(Long id) {
        super(String.format("Bookmark with id=%d not found", id));
    }

    public static BookmarkNotFoundException of(Long id) {
        return new BookmarkNotFoundException(id);
    }
}