package com.example.pbookmark.api.controllers.models;

import jakarta.validation.constraints.NotEmpty;

public record UpdateBookmarkRequest(
        @NotEmpty(message = "Title is required")
        String title,
        @NotEmpty(message = "URL is required")
        String url) {
}