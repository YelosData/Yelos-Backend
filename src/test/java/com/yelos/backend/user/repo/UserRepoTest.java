package com.yelos.backend.user.repo;

import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepoTest {

    @Autowired
    private UserRepo userRepo;

    @Nested
    class ExistsByEmailTests {

        @Test
        void existsByEmail_existingEmail_returnsTrue() {
            userRepo.save(user(
                    "buyer1",
                    "buyer1@example.com",
                    "password123",
                    Role.BUSINESS,
                    BigDecimal.ZERO
            ));

            boolean exists = userRepo.existsByEmail("buyer1@example.com");

            assertThat(exists).isTrue();
        }

        @Test
        void existsByEmail_missingEmail_returnsFalse() {
            boolean exists = userRepo.existsByEmail("missing@example.com");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class ExistsByUsernameTests {

        @Test
        void existsByUsername_existingUsername_returnsTrue() {
            userRepo.save(user(
                    "buyer1",
                    "buyer1@example.com",
                    "password123",
                    Role.BUSINESS,
                    BigDecimal.ZERO
            ));

            boolean exists = userRepo.existsByUsername("buyer1");

            assertThat(exists).isTrue();
        }

        @Test
        void existsByUsername_missingUsername_returnsFalse() {
            boolean exists = userRepo.existsByUsername("missingUser");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    class FindByUsernameTests {

        @Test
        void findByUsername_existingUsername_returnsUser() {
            userRepo.save(user(
                    "buyer1",
                    "buyer1@example.com",
                    "password123",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            ));

            Optional<AppUser> result = userRepo.findByUsername("buyer1");

            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("buyer1");
            assertThat(result.get().getEmail()).isEqualTo("buyer1@example.com");
            assertThat(result.get().getRole()).isEqualTo(Role.BUSINESS);
            assertThat(result.get().getCredits()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        void findByUsername_missingUsername_returnsEmptyOptional() {
            Optional<AppUser> result = userRepo.findByUsername("missingUser");

            assertThat(result).isEmpty();
        }
    }

    private AppUser user(
            String username,
            String email,
            String passwordHash,
            Role role,
            BigDecimal credits
    ) {
        return new AppUser(
                username,
                email,
                passwordHash,
                credits,
                role,
                Instant.now(),
                Instant.now()
        );
    }
}