package com.example.pbookmark.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PagedResult<T>(
        List<T> data,
        long totalElements,
        int pageNumber,
        int totalPages,
        @JsonProperty("isFirst") boolean isFirst,
        @JsonProperty("isLast") boolean isLast,
        @JsonProperty("hasNext") boolean hasNext,

        //defining a logical property for a field that will be used for both serializing and deserializing
        @JsonProperty("hasPrevious") boolean hasPrevious) {}
