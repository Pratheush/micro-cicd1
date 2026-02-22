package com.learncicd.userservice.cache.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for JSON processing errors
 *  You should NOT extend JsonProcessingException for business logic.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
public class BookmarkJsonProcessingException extends JsonProcessingException {

    protected BookmarkJsonProcessingException(String s) {
        super(s);
    }
    public static BookmarkJsonProcessingException bookmarkException(String s) {
        return new BookmarkJsonProcessingException(s);
    }
}
