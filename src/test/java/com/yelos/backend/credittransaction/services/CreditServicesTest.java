package com.yelos.backend.credittransaction.services;

import com.yelos.backend.credittransaction.dto.AddCreditsRequest;
import com.yelos.backend.credittransaction.dto.CreditsReponse;
import com.yelos.backend.transaction.services.TransactionService;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import com.yelos.backend.user.repo.UserRepo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServicesTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private CreditServices creditServices;

    @Nested
    class AddCreditsTests {

        @Test
        void addCredits_validAmount_addsCreditsSavesUserAndCreatesTransaction() {
            Long userId = 1L;

            AppUser user = user(
                    userId,
                    "buyer1",
                    "buyer1@example.com",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            );

            AddCreditsRequest request = new AddCreditsRequest(
                    userId,
                    BigDecimal.valueOf(50)
            );

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));

            CreditsReponse response = creditServices.addCredits(request);

            assertThat(user.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(150));

            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.credits())
                    .isEqualByComparingTo(BigDecimal.valueOf(150));

            verify(userRepo).findById(userId);

            verify(transactionService).AddCreditsTransaction(
                    eq(user),
                    argThat(amount -> amount.compareTo(BigDecimal.valueOf(50)) == 0),
                    argThat(balance -> balance.compareTo(BigDecimal.valueOf(150)) == 0)
            );

            verify(userRepo).save(user);
        }

        @Test
        void addCredits_zeroAmount_succeedsButCreditsUnchanged() {
            Long userId = 1L;

            AppUser user = user(
                    userId,
                    "buyer1",
                    "buyer1@example.com",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            );

            AddCreditsRequest request = new AddCreditsRequest(
                    userId,
                    BigDecimal.ZERO
            );

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));

            CreditsReponse response = creditServices.addCredits(request);

            assertThat(user.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(100));

            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.credits())
                    .isEqualByComparingTo(BigDecimal.valueOf(100));

            verify(userRepo).findById(userId);

            verify(transactionService).AddCreditsTransaction(
                    eq(user),
                    argThat(amount -> amount.compareTo(BigDecimal.ZERO) == 0),
                    argThat(balance -> balance.compareTo(BigDecimal.valueOf(100)) == 0)
            );

            verify(userRepo).save(user);
        }

        @Test
        void addCredits_negativeAmount_throwsException() {
            Long userId = 1L;

            AddCreditsRequest request = new AddCreditsRequest(
                    userId,
                    BigDecimal.valueOf(-10)
            );

            assertThatThrownBy(() -> creditServices.addCredits(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Invalid amount added");

            verifyNoInteractions(userRepo);
            verifyNoInteractions(transactionService);
        }

        @Test
        void addCredits_userNotFound_throwsException() {
            Long userId = 1L;

            AddCreditsRequest request = new AddCreditsRequest(
                    userId,
                    BigDecimal.valueOf(50)
            );

            when(userRepo.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> creditServices.addCredits(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");

            verify(userRepo).findById(userId);
            verify(userRepo, never()).save(any(AppUser.class));
            verifyNoInteractions(transactionService);
        }
    }

    private AppUser user(
            Long id,
            String username,
            String email,
            Role role,
            BigDecimal credits
    ) {
        AppUser user = new AppUser(
                username,
                email,
                "password123",
                credits,
                role,
                Instant.now(),
                Instant.now()
        );

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }
}