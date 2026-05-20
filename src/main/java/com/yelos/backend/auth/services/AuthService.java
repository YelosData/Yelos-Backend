package com.yelos.backend.auth.services;

import com.yelos.backend.auth.dto.AuthResponse;
import com.yelos.backend.auth.dto.LoginRequest;
import com.yelos.backend.auth.dto.RegisterRequest;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import com.yelos.backend.user.repo.UserRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class AuthService {
    private final UserRepo userRepo;

    public AuthService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepo.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists");
        }
        if (request.role() == Role.ADMIN) {
            throw new RuntimeException("Cannot register as admin");
        }
        String passwordHash = request.password();
        AppUser user = new AppUser(
                request.username(),
                request.email(),
                passwordHash,
                BigDecimal.ZERO,
                request.role(),
                Instant.now(),
                Instant.now()
        );
        AppUser savedUser = userRepo.save(user);
        return new AuthResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getCredits()
        );
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepo.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("No such user exists"));

        if (!user.getPasswordHash().equals(request.password())) {
            throw new RuntimeException("Invalid Password");
        }

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCredits()
        );
    }

}
