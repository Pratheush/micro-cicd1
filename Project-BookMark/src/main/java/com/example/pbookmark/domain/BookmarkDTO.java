package com.example.pbookmark.domain;

import java.io.Serializable;
import java.time.Instant;

public record BookmarkDTO(
        Long id,
        String title,
        String url,
        Instant createdAt,
        String createdBy,
        String updatedBy) implements Serializable {
    static BookmarkDTO from(Bookmark bookmark) {
        return new BookmarkDTO(bookmark.getId(),
                bookmark.getTitle(),
                bookmark.getUrl(),
                bookmark.getCreatedAt(),
                bookmark.getCreatedBy(),
                bookmark.getUpdatedBy()
        );
    }
}
