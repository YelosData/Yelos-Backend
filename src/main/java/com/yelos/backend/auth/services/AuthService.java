package com.yelos.backend.auth.services;

import com.yelos.backend.auth.dto.AuthResponse;
import com.yelos.backend.auth.dto.LoginRequest;
import com.yelos.backend.auth.dto.RegisterRequest;
import com.yelos.backend.security.JwtService;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import com.yelos.backend.user.repo.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepo userRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already exists"
            );
        }

        if (userRepo.existsByUsername(request.username())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot register as admin"
            );
        }

        String passwordHash = passwordEncoder.encode(request.password());

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

        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getCredits(),
                accessToken,
                refreshToken
        );
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = userRepo.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Username does not exist"
                ));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid password"
            );
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCredits(),
                accessToken,
                refreshToken
        );
    }
}