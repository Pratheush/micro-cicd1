package com.example.pbookmark.domain;

public record UpdateBookmarkCommand(
        Long id,
        String title,
        String url) {
}
