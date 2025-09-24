package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.dto.LoginRequest;
import com.evoting.evoting_backend.dto.LoginResponse;
import com.evoting.evoting_backend.model.User;
import com.evoting.evoting_backend.security.JwtUtil;
import com.evoting.evoting_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired private UserService userService;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody User user) {
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User savedUser = userService.saveUser(user);
            ApiResponse response = new ApiResponse(true, "User registered successfully", savedUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse response = new ApiResponse(false, "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            Optional<User> userOpt = userService.getByUsername(loginRequest.getUsername());
            
            if (userOpt.isEmpty() || !passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPassword())) {
                ApiResponse response = new ApiResponse(false, "Invalid credentials");
                return ResponseEntity.status(401).body(response);
            }
            
            User user = userOpt.get();
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
            LoginResponse loginResponse = new LoginResponse(token, "Login successful", user.getRole().name(), user.getUsername());
            
            ApiResponse response = new ApiResponse(true, "Login successful", loginResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse response = new ApiResponse(false, "Login failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}