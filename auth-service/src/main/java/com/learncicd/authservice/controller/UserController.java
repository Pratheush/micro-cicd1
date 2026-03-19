package com.learncicd.authservice.controller;

import com.learncicd.authservice.exception.BadRequestException;
import com.learncicd.authservice.model.JwtTokenResponse;
import com.learncicd.authservice.model.LoginRequest;
import com.learncicd.authservice.model.User;
import com.learncicd.authservice.model.UserDTO;
import com.learncicd.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register-user")
    public ResponseEntity<UserDTO> registerUser(@RequestBody User user){
        log.info("Received request to register user: {}", user);
        UserDTO userDTO = userService.saveUser(user);
        log.info("User registered successfully: {}", userDTO);
        return new ResponseEntity<>(userDTO, HttpStatus.CREATED);
    }

    @PostMapping("/generate-token")
    public JwtTokenResponse generateToken(@RequestBody LoginRequest loginRequest){
        //log.info("Auth-Service UserController : generateToken : {}",loginRequest);
        try {
            // Validate Passwords at Login. Spring Security compares the raw password with the stored hash using the same encoder
            // Internally, PasswordEncoder.matches(rawPassword, encodedPassword) is called.
            // BCrypt extracts the salt from the stored hash, re-hashes the raw password with that salt, and checks if it matches.
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            if (authentication.isAuthenticated()) return userService.generateToken(loginRequest.getUsername());
            else throw new BadRequestException("Invalid Credentials");
        }  catch (BadRequestException e) {
            throw new BadRequestException("Invalid Credentials");
        }
    }
}
