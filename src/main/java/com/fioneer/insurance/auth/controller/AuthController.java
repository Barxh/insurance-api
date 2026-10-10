package com.fioneer.insurance.auth.controller;

import com.fioneer.insurance.auth.dto.AuthResponse;
import com.fioneer.insurance.auth.dto.LoginRequest;
import com.fioneer.insurance.auth.dto.RefreshRequest;
import com.fioneer.insurance.auth.dto.RegisterRequest;
import com.fioneer.insurance.auth.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Valid @RequestBody RefreshRequest request
    ) {
        authService.logout(request);
    }
}
