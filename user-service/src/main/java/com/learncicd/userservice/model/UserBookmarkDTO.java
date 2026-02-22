package com.learncicd.userservice;

public record UserBookmarkDTO(String username,
                              String email,
                              Long bookmarkId,
                              String title,
                              String url) {
}
