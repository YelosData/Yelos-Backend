package com.yelos.backend.auth.dto;

import com.yelos.backend.user.models.Role;

public record RegisterRequest(
    String username,
    String email,
    String password,
    Role role
) {}
