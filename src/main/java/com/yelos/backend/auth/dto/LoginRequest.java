package com.yelos.backend.auth.dto;

public record LoginRequest(
        String username,
        String password
) {
}
