package com.yelos.backend.security;

public record AuthUserPrincipal(
        Long userId,
        String username,
        String role
) {
}