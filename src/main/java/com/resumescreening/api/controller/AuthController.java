package com.resumescreening.api.controller;

import com.resumescreening.api.model.dto.request.LoginRequest;
import com.resumescreening.api.model.dto.request.RegisterRequest;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.AuthResponse;
import com.resumescreening.api.model.dto.response.UserResponse;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.security.jwt.JwtUtil;
import com.resumescreening.api.service.UserService;
import com.resumescreening.api.util.DtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // Register new user
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        // Register user
        User user = userService.registerUser(
                request.getEmail(),
                request.getPassword(),
                request.getFullName(),
                request.getRole(),
                request.getCompanyName(),
                request.getDesignation()
        );

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        // Build response
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400000L) // 24 hours in milliseconds
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .companyName(user.getCompanyName())
                .isEmailVerified(user.getIsEmailVerified())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", authResponse));
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get user details
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate token
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getId(),
                user.getRole().name()
        );

        // Build response
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .companyName(user.getCompanyName())
                .isEmailVerified(user.getIsEmailVerified())
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", authResponse)
        );
    }

    // Get current user info
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assert authentication != null;
        String email = authentication.getName();

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse userResponse = DtoMapper.toUserResponse(user);

        return ResponseEntity.ok(
                ApiResponse.success(userResponse)
        );
    }
}