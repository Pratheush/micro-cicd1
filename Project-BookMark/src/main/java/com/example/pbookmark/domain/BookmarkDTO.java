package com.example.pbookmark.domain;

import java.io.Serializable;
import java.time.Instant;

public record BookmarkDTO(
        Long id,
        String title,
        String url,
        Instant createdAt) implements Serializable {
    static BookmarkDTO from(Bookmark bookmark) {
        return new BookmarkDTO(bookmark.getId(),
                bookmark.getTitle(),
                bookmark.getUrl(),
                bookmark.getCreatedAt()
        );
    }
}
