package com.learncicd.userservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/health")
    public String getUserHealth() {
        log.info("API Request: Get User Health");
        return "USER-UP-Healthy";
    }

     // user info should be get with login using auth-service via api-gateway
    // Fetch bookmark with user info : getUserBookmark : validation if bookmark exist or not
    // Create bookmark with validation :  Validation: bookmark title must not exist before
    // Update bookmark with validation : check if bookmark with the title exist
    // Delete bookmark with validation : check if bookmark exist with the title before deleting

}
