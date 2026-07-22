package com.yelos.backend.auth.controllers;

import com.yelos.backend.auth.dto.AuthResponse;
import com.yelos.backend.auth.dto.LoginRequest;
import com.yelos.backend.auth.dto.RegisterRequest;
import com.yelos.backend.auth.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            String message = e.getReason() != null
                    ? e.getReason()
                    : "Registration failed";

            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of(
                            "status", e.getStatusCode().value(),
                            "message", message
                    ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            String message = e.getReason() != null
                    ? e.getReason()
                    : "Login failed";

            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(Map.of(
                            "status", e.getStatusCode().value(),
                            "message", message
                    ));
        }
    }
}