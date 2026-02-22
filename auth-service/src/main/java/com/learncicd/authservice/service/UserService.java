package com.learncicd.authservice.service;

import com.learncicd.authservice.model.JwtTokenResponse;
import com.learncicd.authservice.model.User;
import com.learncicd.authservice.model.UserDTO;
import com.learncicd.authservice.repository.UserRepo;
import com.learncicd.authservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserDTO saveUser(User user){
        log.info("Received request to save user: {}", user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepo.save(user);
        log.info("User saved successfully: {}", savedUser);
        return new UserDTO(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getUsername(),
                savedUser.getRoles().name()
        );
    }

    public JwtTokenResponse generateToken(String username){
        String token = jwtUtil.generateToken(username);
        JwtTokenResponse jwtTokenResponse = new JwtTokenResponse();
        jwtTokenResponse.setToken(token);
        jwtTokenResponse.setType("Bearer");
        jwtTokenResponse.setValidUntil(jwtUtil.expirationDate(token).toString());
        return jwtTokenResponse;
    }
}
