package com.yelos.backend.auth;

import com.yelos.backend.IntegrationTestBase;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    class RegisterTests {

        @Test
        void register_validBusinessUser_savesUserAndReturnsAuthResponse() throws Exception {
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
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.username").value("buyer1"))
                    .andExpect(jsonPath("$.email").value("buyer1@example.com"))
                    .andExpect(jsonPath("$.role").value("BUSINESS"))
                    .andExpect(jsonPath("$.credits").value(0))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists());

            AppUser savedUser = userRepo.findByUsername("buyer1").orElseThrow();

            assertThat(savedUser.getUsername()).isEqualTo("buyer1");
            assertThat(savedUser.getEmail()).isEqualTo("buyer1@example.com");

            assertThat(savedUser.getPasswordHash())
                    .isNotEqualTo("password123");

            assertThat(passwordEncoder.matches("password123", savedUser.getPasswordHash()))
                    .isTrue();

            assertThat(savedUser.getRole()).isEqualTo(Role.BUSINESS);
            assertThat(savedUser.getCredits()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void register_validSellerUser_savesUserAndReturnsAuthResponse() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "seller1",
                                      "email": "seller1@example.com",
                                      "password": "password123",
                                      "role": "SELLER"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.username").value("seller1"))
                    .andExpect(jsonPath("$.email").value("seller1@example.com"))
                    .andExpect(jsonPath("$.role").value("SELLER"))
                    .andExpect(jsonPath("$.credits").value(0))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists());

            AppUser savedUser = userRepo.findByUsername("seller1").orElseThrow();

            assertThat(savedUser.getUsername()).isEqualTo("seller1");
            assertThat(savedUser.getEmail()).isEqualTo("seller1@example.com");

            assertThat(savedUser.getPasswordHash())
                    .isNotEqualTo("password123");

            assertThat(passwordEncoder.matches("password123", savedUser.getPasswordHash()))
                    .isTrue();

            assertThat(savedUser.getRole()).isEqualTo(Role.SELLER);
            assertThat(savedUser.getCredits()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void register_duplicateEmail_returnsConflictAndDoesNotSaveSecondUser() throws Exception {
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
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "buyer2",
                                      "email": "buyer1@example.com",
                                      "password": "password456",
                                      "role": "BUSINESS"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Email already exists"));

            assertThat(userRepo.findAll()).hasSize(1);
            assertThat(userRepo.existsByUsername("buyer2")).isFalse();
        }

        @Test
        void register_duplicateUsername_returnsConflictAndDoesNotSaveSecondUser() throws Exception {
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
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "buyer1",
                                      "email": "buyer2@example.com",
                                      "password": "password456",
                                      "role": "BUSINESS"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Username already exists"));

            assertThat(userRepo.findAll()).hasSize(1);
            assertThat(userRepo.existsByEmail("buyer2@example.com")).isFalse();
        }

        @Test
        void register_adminRole_returnsBadRequestAndDoesNotSaveUser() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "admin1",
                                      "email": "admin1@example.com",
                                      "password": "password123",
                                      "role": "ADMIN"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Cannot register as admin"));

            assertThat(userRepo.findAll()).isEmpty();
        }
    }

    @Nested
    class LoginTests {

        @Test
        void login_validCredentials_returnsAuthResponse() throws Exception {
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
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "buyer1",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.username").value("buyer1"))
                    .andExpect(jsonPath("$.email").value("buyer1@example.com"))
                    .andExpect(jsonPath("$.role").value("BUSINESS"))
                    .andExpect(jsonPath("$.credits").value(0))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists());
        }

        @Test
        void login_wrongPassword_returnsUnauthorized() throws Exception {
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
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "buyer1",
                                      "password": "wrongPassword"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid password"));
        }

        @Test
        void login_missingUser_returnsNotFound() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "missingUser",
                                      "password": "password123"
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Username does not exist"));
        }
    }
}