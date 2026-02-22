package com.learncicd.userservice.model;

import java.io.Serializable;

public record UserBookmarkDTO(String username,
                              String email,
                              Long bookmarkId,
                              String title,
                              String url) implements Serializable {
}
