package com.yelos.backend.auth.controllers;

import com.yelos.backend.auth.dto.AuthResponse;
import com.yelos.backend.auth.dto.LoginRequest;
import com.yelos.backend.auth.dto.RegisterRequest;
import com.yelos.backend.auth.services.AuthService;
import com.yelos.backend.security.JwtAuthFilter;
import com.yelos.backend.user.models.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @Nested
    class RegisterTests {

        @Test
        void register_validRequest_returnsAuthResponse() throws Exception {
            AuthResponse response = new AuthResponse(
                    1L,
                    "buyer1",
                    "buyer1@example.com",
                    Role.BUSINESS,
                    BigDecimal.ZERO,
                    "access-token",
                    "refresh-token"
            );

            when(authService.register(any(RegisterRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "buyer1",
                                      "email": "buyer1@example.com",
                                      "password": "password123",
                                      "role": "BUSINESS"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.username").value("buyer1"))
                    .andExpect(jsonPath("$.email").value("buyer1@example.com"))
                    .andExpect(jsonPath("$.role").value("BUSINESS"))
                    .andExpect(jsonPath("$.credits").value(0))
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

            verify(authService).register(argThat(request ->
                    request.username().equals("buyer1")
                            && request.email().equals("buyer1@example.com")
                            && request.password().equals("password123")
                            && request.role() == Role.BUSINESS
            ));
        }

        @Test
        void register_serviceThrowsException_exceptionBubblesUp() {
            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new RuntimeException("Email already exists"));

            assertThatThrownBy(() ->
                    mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "buyer1",
                                      "email": "buyer1@example.com",
                                      "password": "password123",
                                      "role": "BUSINESS"
                                    }
                                    """))
            )
                    .hasRootCauseInstanceOf(RuntimeException.class)
                    .hasRootCauseMessage("Email already exists");

            verify(authService).register(argThat(request ->
                    request.username().equals("buyer1")
                            && request.email().equals("buyer1@example.com")
                            && request.password().equals("password123")
                            && request.role() == Role.BUSINESS
            ));
        }
    }

    @Nested
    class LoginTests {

        @Test
        void login_validRequest_returnsAuthResponse() throws Exception {
            AuthResponse response = new AuthResponse(
                    1L,
                    "buyer1",
                    "buyer1@example.com",
                    Role.BUSINESS,
                    BigDecimal.valueOf(50),
                    "access-token",
                    "refresh-token"
            );

            when(authService.login(any(LoginRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "buyer1",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.username").value("buyer1"))
                    .andExpect(jsonPath("$.email").value("buyer1@example.com"))
                    .andExpect(jsonPath("$.role").value("BUSINESS"))
                    .andExpect(jsonPath("$.credits").value(50))
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

            verify(authService).login(argThat(request ->
                    request.username().equals("buyer1")
                            && request.password().equals("password123")
            ));
        }

        @Test
        void login_serviceThrowsException_exceptionBubblesUp() {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new RuntimeException("Invalid Password"));

            assertThatThrownBy(() ->
                    mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "buyer1",
                                      "password": "wrongPassword"
                                    }
                                    """))
            )
                    .hasRootCauseInstanceOf(RuntimeException.class)
                    .hasRootCauseMessage("Invalid Password");

            verify(authService).login(argThat(request ->
                    request.username().equals("buyer1")
                            && request.password().equals("wrongPassword")
            ));
        }
    }
}