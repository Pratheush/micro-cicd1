package com.learncicd.userservice.model;

import java.io.Serializable;
import java.time.Instant;

public record UserBookmarkDTO(String username,
                              String email,
                              Long bookmarkId,
                              String title,
                              String url,
                              Instant createdAt,
                              String createdBy,
                              String updatedBy) implements Serializable {
}
