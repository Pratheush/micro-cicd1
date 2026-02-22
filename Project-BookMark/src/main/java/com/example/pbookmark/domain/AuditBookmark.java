package com.example.pbookmark.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

public class AuditBookmark {

    @PrePersist
    @PreUpdate
    private void beforeAnyUpdate(Bookmark bookmark) {
        bookmark.setUpdatedAt(Instant.now());
    }
}
