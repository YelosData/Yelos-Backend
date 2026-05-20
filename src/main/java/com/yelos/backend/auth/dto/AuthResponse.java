package com.yelos.backend.auth.dto;

import com.yelos.backend.user.models.Role;

import java.math.BigDecimal;

public record AuthResponse(
        Long id,
        String username,
        String email,
        Role role,
        BigDecimal credits
) {
}
