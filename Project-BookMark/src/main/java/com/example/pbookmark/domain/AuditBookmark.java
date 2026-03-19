package com.example.pbookmark.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

public class AuditBookmark {

    @PrePersist
    public void beforeCreate(Bookmark bookmark) {
        bookmark.setCreatedAt(Instant.now());
        // createdBy is set in service layer from Authentication
    }

    @PreUpdate
    public void beforeUpdate(Bookmark bookmark) {
        bookmark.setUpdatedAt(Instant.now());
        // updatedBy is set in service layer from Authentication
    }
}
