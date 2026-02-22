package com.learncicd.userservice.client;

import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;

public record CreateBookmarkRequest(
        @NotEmpty(message = "Title is Required")
        String title,
        @NotEmpty(message = "URL is Required")
        String url
) implements Serializable {}
