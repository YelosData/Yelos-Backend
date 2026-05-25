
package com.yelos.backend.credittransaction;

import com.yelos.backend.IntegrationTestBase;
import com.yelos.backend.transaction.models.Transaction;
import com.yelos.backend.transaction.models.TransactionType;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CreditTransactionIntegrationTest extends IntegrationTestBase {

    @Nested
    class AddCreditsTests {

        @Test
        void addCredits_validAmount_updatesUserCreditsAndSavesTransaction() throws Exception {
            AppUser user = saveUser(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            );

            mockMvc.perform(post("/api/credittransaction/add")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": %d,
                                      "credits": 50
                                    }
                                    """.formatted(user.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(user.getId()))
                    .andExpect(jsonPath("$.credits").value(150));

            AppUser updatedUser = userRepo.findById(user.getId()).orElseThrow();

            assertThat(updatedUser.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(150));

            List<Transaction> transactions = transactionRepo.findAll();

            assertThat(transactions).hasSize(1);

            Transaction transaction = transactions.get(0);

            assertThat(getField(transaction, "type"))
                    .isEqualTo(TransactionType.ADD_CREDIT);

            assertThat(userIdOf(transaction))
                    .isEqualTo(user.getId());

            assertThat(getField(transaction, "purchase"))
                    .isNull();

            assertThat(bigDecimalEquals(getField(transaction, "amount"), BigDecimal.valueOf(50)))
                    .isTrue();

            assertThat(bigDecimalEquals(getField(transaction, "balanceAfter"), BigDecimal.valueOf(150)))
                    .isTrue();

            assertThat(getField(transaction, "createdAt"))
                    .isNotNull();
        }

        @Test
        void addCredits_zeroAmount_succeedsButCreditsUnchanged() throws Exception {
            AppUser user = saveUser(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            );

            mockMvc.perform(post("/api/credittransaction/add")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": %d,
                                      "credits": 0
                                    }
                                    """.formatted(user.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(user.getId()))
                    .andExpect(jsonPath("$.credits").value(100));

            AppUser updatedUser = userRepo.findById(user.getId()).orElseThrow();

            assertThat(updatedUser.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(100));

            List<Transaction> transactions = transactionRepo.findAll();

            assertThat(transactions).hasSize(1);

            Transaction transaction = transactions.get(0);

            assertThat(getField(transaction, "type"))
                    .isEqualTo(TransactionType.ADD_CREDIT);

            assertThat(userIdOf(transaction))
                    .isEqualTo(user.getId());

            assertThat(bigDecimalEquals(getField(transaction, "amount"), BigDecimal.ZERO))
                    .isTrue();

            assertThat(bigDecimalEquals(getField(transaction, "balanceAfter"), BigDecimal.valueOf(100)))
                    .isTrue();
        }

        @Test
        void addCredits_negativeAmount_throwsExceptionAndDoesNotUpdateUserOrSaveTransaction() {
            AppUser user = saveUser(
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(100)
            );

            assertThatThrownBy(() ->
                    mockMvc.perform(post("/api/credittransaction/add")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": %d,
                                      "credits": -50
                                    }
                                    """.formatted(user.getId())))
            )
                    .hasRootCauseInstanceOf(RuntimeException.class)
                    .hasRootCauseMessage("Invalid amount added");

            AppUser updatedUser = userRepo.findById(user.getId()).orElseThrow();

            assertThat(updatedUser.getCredits())
                    .isEqualByComparingTo(BigDecimal.valueOf(100));

            assertThat(transactionRepo.findAll())
                    .isEmpty();
        }

        @Test
        void addCredits_missingUser_throwsExceptionAndDoesNotSaveTransaction() {
            assertThatThrownBy(() ->
                    mockMvc.perform(post("/api/credittransaction/add")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "userId": 999,
                                      "credits": 50
                                    }
                                    """))
            )
                    .hasRootCauseInstanceOf(RuntimeException.class)
                    .hasRootCauseMessage("User not found");

            assertThat(transactionRepo.findAll())
                    .isEmpty();
        }
    }

    private AppUser saveUser(
            String username,
            Role role,
            BigDecimal credits
    ) {
        return userRepo.save(new AppUser(
                username,
                username + "@example.com",
                "password123",
                credits,
                role,
                Instant.now(),
                Instant.now()
        ));
    }

    private Object getField(Object target, String fieldName) {
        return ReflectionTestUtils.getField(target, fieldName);
    }

    private Long userIdOf(Transaction transaction) {
        AppUser user = (AppUser) getField(transaction, "user");
        return user.getId();
    }

    private boolean bigDecimalEquals(Object actual, BigDecimal expected) {
        if (!(actual instanceof BigDecimal actualBigDecimal)) {
            return false;
        }

        return actualBigDecimal.compareTo(expected) == 0;
    }
}