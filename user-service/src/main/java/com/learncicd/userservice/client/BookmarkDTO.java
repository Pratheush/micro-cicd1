package com.learncicd.userservice.client;

import java.io.Serializable;
import java.time.Instant;

public record BookmarkDTO(
        Long id,
        String title,
        String url,
        Instant createdAt,
        String createdBy,
        String updatedBy) implements Serializable {

}
