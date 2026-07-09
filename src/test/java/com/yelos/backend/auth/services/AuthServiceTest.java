package com.yelos.backend.auth.services;

import com.yelos.backend.auth.dto.AuthResponse;
import com.yelos.backend.auth.dto.LoginRequest;
import com.yelos.backend.auth.dto.RegisterRequest;
import com.yelos.backend.security.JwtService;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import com.yelos.backend.user.repo.UserRepo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Nested
    class RegisterTests {

        @Test
        void register_validRequest_hashesPasswordSavesUserAndReturnsAuthResponseWithTokens() {
            RegisterRequest request = new RegisterRequest(
                    "buyer1",
                    "buyer1@example.com",
                    "password123",
                    Role.BUSINESS
            );

            when(userRepo.existsByEmail("buyer1@example.com")).thenReturn(false);
            when(userRepo.existsByUsername("buyer1")).thenReturn(false);

            when(passwordEncoder.encode("password123"))
                    .thenReturn("hashedPassword123");

            when(userRepo.save(any(AppUser.class))).thenAnswer(invocation -> {
                AppUser savedUser = invocation.getArgument(0);
                ReflectionTestUtils.setField(savedUser, "id", 1L);
                return savedUser;
            });

            when(jwtService.generateAccessToken(any(AppUser.class)))
                    .thenReturn("access-token");

            when(jwtService.generateRefreshToken(any(AppUser.class)))
                    .thenReturn("refresh-token");

            AuthResponse response = authService.register(request);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.username()).isEqualTo("buyer1");
            assertThat(response.email()).isEqualTo("buyer1@example.com");
            assertThat(response.role()).isEqualTo(Role.BUSINESS);
            assertThat(response.credits()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");

            verify(userRepo).existsByEmail("buyer1@example.com");
            verify(userRepo).existsByUsername("buyer1");

            verify(passwordEncoder).encode("password123");

            verify(userRepo).save(argThat(user ->
                    user.getUsername().equals("buyer1")
                            && user.getEmail().equals("buyer1@example.com")
                            && user.getPasswordHash().equals("hashedPassword123")
                            && user.getRole() == Role.BUSINESS
                            && user.getCredits().compareTo(BigDecimal.ZERO) == 0
            ));

            verify(jwtService).generateAccessToken(argThat(user ->
                    user.getId() == 1L
                            && user.getUsername().equals("buyer1")
            ));

            verify(jwtService).generateRefreshToken(argThat(user ->
                    user.getId() == 1L
                            && user.getUsername().equals("buyer1")
            ));
        }

        @Test
        void register_emailAlreadyExists_throwsException() {
            RegisterRequest request = new RegisterRequest(
                    "buyer1",
                    "buyer1@example.com",
                    "password123",
                    Role.BUSINESS
            );

            when(userRepo.existsByEmail("buyer1@example.com")).thenReturn(true);

            Throwable thrown = catchThrowable(() -> authService.register(request));

            assertResponseStatusException(
                    thrown,
                    HttpStatus.CONFLICT,
                    "Email already exists"
            );

            verify(userRepo).existsByEmail("buyer1@example.com");
            verify(userRepo, never()).existsByUsername(anyString());
            verify(userRepo, never()).save(any(AppUser.class));

            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(jwtService);
        }

        @Test
        void register_usernameAlreadyExists_throwsException() {
            RegisterRequest request = new RegisterRequest(
                    "buyer1",
                    "buyer1@example.com",
                    "password123",
                    Role.BUSINESS
            );

            when(userRepo.existsByEmail("buyer1@example.com")).thenReturn(false);
            when(userRepo.existsByUsername("buyer1")).thenReturn(true);

            Throwable thrown = catchThrowable(() -> authService.register(request));

            assertResponseStatusException(
                    thrown,
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );

            verify(userRepo).existsByEmail("buyer1@example.com");
            verify(userRepo).existsByUsername("buyer1");
            verify(userRepo, never()).save(any(AppUser.class));

            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(jwtService);
        }

        @Test
        void register_adminRole_throwsException() {
            RegisterRequest request = new RegisterRequest(
                    "admin1",
                    "admin1@example.com",
                    "password123",
                    Role.ADMIN
            );

            when(userRepo.existsByEmail("admin1@example.com")).thenReturn(false);
            when(userRepo.existsByUsername("admin1")).thenReturn(false);

            Throwable thrown = catchThrowable(() -> authService.register(request));

            assertResponseStatusException(
                    thrown,
                    HttpStatus.BAD_REQUEST,
                    "Cannot register as admin"
            );

            verify(userRepo).existsByEmail("admin1@example.com");
            verify(userRepo).existsByUsername("admin1");
            verify(userRepo, never()).save(any(AppUser.class));

            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(jwtService);
        }
    }

    @Nested
    class LoginTests {

        @Test
        void login_validCredentials_returnsAuthResponseWithTokens() {
            LoginRequest request = new LoginRequest(
                    "buyer1",
                    "password123"
            );

            AppUser user = user(
                    1L,
                    "buyer1",
                    "buyer1@example.com",
                    "hashedPassword123",
                    Role.BUSINESS,
                    BigDecimal.valueOf(50)
            );

            when(userRepo.findByUsername("buyer1"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(
                    eq("password123"),
                    eq("hashedPassword123")
            ))
                    .thenReturn(true);

            when(jwtService.generateAccessToken(any(AppUser.class)))
                    .thenReturn("access-token");

            when(jwtService.generateRefreshToken(any(AppUser.class)))
                    .thenReturn("refresh-token");

            AuthResponse response = authService.login(request);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.username()).isEqualTo("buyer1");
            assertThat(response.email()).isEqualTo("buyer1@example.com");
            assertThat(response.role()).isEqualTo(Role.BUSINESS);
            assertThat(response.credits()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");

            verify(userRepo).findByUsername("buyer1");

            verify(passwordEncoder).matches(
                    eq("password123"),
                    eq("hashedPassword123")
            );

            verify(jwtService).generateAccessToken(argThat(savedUser ->
                    savedUser.getId() == 1L
                            && savedUser.getUsername().equals("buyer1")
                            && savedUser.getEmail().equals("buyer1@example.com")
                            && savedUser.getRole() == Role.BUSINESS
            ));

            verify(jwtService).generateRefreshToken(argThat(savedUser ->
                    savedUser.getId() == 1L
                            && savedUser.getUsername().equals("buyer1")
                            && savedUser.getEmail().equals("buyer1@example.com")
                            && savedUser.getRole() == Role.BUSINESS
            ));
        }

        @Test
        void login_userNotFound_throwsException() {
            LoginRequest request = new LoginRequest(
                    "missingUser",
                    "password123"
            );

            when(userRepo.findByUsername("missingUser"))
                    .thenReturn(Optional.empty());

            Throwable thrown = catchThrowable(() -> authService.login(request));

            assertResponseStatusException(
                    thrown,
                    HttpStatus.NOT_FOUND,
                    "Username does not exist"
            );

            verify(userRepo).findByUsername("missingUser");
            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(jwtService);
        }

        @Test
        void login_wrongPassword_throwsException() {
            LoginRequest request = new LoginRequest(
                    "buyer1",
                    "wrongPassword"
            );

            AppUser user = user(
                    1L,
                    "buyer1",
                    "buyer1@example.com",
                    "hashedPassword123",
                    Role.BUSINESS,
                    BigDecimal.ZERO
            );

            when(userRepo.findByUsername("buyer1"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(
                    eq("wrongPassword"),
                    eq("hashedPassword123")
            ))
                    .thenReturn(false);

            Throwable thrown = catchThrowable(() -> authService.login(request));

            assertResponseStatusException(
                    thrown,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid password"
            );

            verify(userRepo).findByUsername("buyer1");

            verify(passwordEncoder).matches(
                    eq("wrongPassword"),
                    eq("hashedPassword123")
            );

            verifyNoInteractions(jwtService);
        }
    }

    private void assertResponseStatusException(
            Throwable throwable,
            HttpStatus expectedStatus,
            String expectedReason
    ) {
        assertThat(throwable)
                .isInstanceOf(ResponseStatusException.class);

        ResponseStatusException exception = (ResponseStatusException) throwable;

        assertThat(exception.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(exception.getReason()).isEqualTo(expectedReason);
    }

    private AppUser user(
            Long id,
            String username,
            String email,
            String passwordHash,
            Role role,
            BigDecimal credits
    ) {
        AppUser user = new AppUser(
                username,
                email,
                passwordHash,
                credits,
                role,
                Instant.now(),
                Instant.now()
        );

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }
}