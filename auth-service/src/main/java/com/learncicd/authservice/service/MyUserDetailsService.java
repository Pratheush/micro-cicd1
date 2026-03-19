package com.learncicd.authservice.service;


import com.learncicd.authservice.model.User;
import com.learncicd.authservice.repository.UserRepo;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


/**
 * this is mainly responsible for fetching user from db
 */

@Slf4j
//@Service
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;

    public MyUserDetailsService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Nonnull
    @Override
    @Cacheable(value = "usersByUserName", key = "#username")  // ✅ production-grade caching
    public UserDetails loadUserByUsername(@Nonnull String username) throws UsernameNotFoundException {
        log.info("MyUserDetailsService: loadUserByUsername Received request to load user by username: {}", username);

        User user = userRepo.findByUsername(username).orElseThrow(() -> {
            log.warn("{} Not Found",username);
            return new UsernameNotFoundException("User Not Found");
        });
        log.info("MyUserDetailsService: loadUserByUsername User found: {}", user.getUsername());
        return new MyUserDetails(user);

    }
}
