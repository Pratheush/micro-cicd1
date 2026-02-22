package com.learncicd.userservice.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public record PagedResult<T>(
        List<T> data,
        long totalElements,
        int pageNumber,
        int totalPages,
        @JsonProperty("isFirst") boolean isFirst,
        @JsonProperty("isLast") boolean isLast,
        @JsonProperty("hasNext") boolean hasNext,

        //defining a logical property for a field that will be used for both serializing and deserializing
        @JsonProperty("hasPrevious") boolean hasPrevious,
        Optional<String> message) implements Serializable {
    public static PagedResult<BookmarkDTO> empty() {
        return new PagedResult<>(List.of(), 0, 0, 0, true, true, false, false,Optional.empty());
    }

    public static PagedResult<BookmarkDTO> withMessage(String s) {
        return new PagedResult<>(List.of(), 0, 0, 0, true, true, false, false, Optional.of(s));
    }
}
