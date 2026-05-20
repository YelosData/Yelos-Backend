package com.yelos.backend.auth.controllers;

import com.yelos.backend.auth.services.AuthService;
import com.yelos.backend.auth.dto.AuthResponse;
import com.yelos.backend.auth.dto.LoginRequest;
import com.yelos.backend.auth.dto.RegisterRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
