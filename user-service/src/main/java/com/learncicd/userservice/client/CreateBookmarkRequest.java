package com.example.pbookmark.api.controllers.models;

import jakarta.validation.constraints.NotEmpty;

public record CreateBookmarkRequest(
        @NotEmpty(message = "Title is Required")
        String title,
        @NotEmpty(message = "URL is Required")
        String url
) {}
