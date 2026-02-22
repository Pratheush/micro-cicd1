package com.learncicd.userservice.client;

import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;

public record UpdateBookmarkRequest(
        @NotEmpty(message = "Title is required")
        String title,
        @NotEmpty(message = "URL is required")
        String url) implements Serializable {
}