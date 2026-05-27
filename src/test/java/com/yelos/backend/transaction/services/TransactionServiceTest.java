package com.yelos.backend.transaction.services;

import com.yelos.backend.purchase.models.Purchase;
import com.yelos.backend.transaction.models.Transaction;
import com.yelos.backend.transaction.models.TransactionType;
import com.yelos.backend.transaction.repo.TransactionRepo;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.models.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepo transactionRepo;

    @InjectMocks
    private TransactionService transactionService;

    @Nested
    class PurchaseTransactionTests {

        @Test
        void purchaseTransaction_validPurchase_savesBuyerDebitAndSellerCreditTransactions() {
            AppUser buyer = user(
                    1L,
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(70)
            );

            AppUser seller = user(
                    2L,
                    "seller1",
                    Role.SELLER,
                    BigDecimal.valueOf(30)
            );

            Purchase purchase = mock(Purchase.class);

            BigDecimal amount = BigDecimal.valueOf(30);

            transactionService.PurchaseTransaction(
                    buyer,
                    seller,
                    purchase,
                    amount
            );

            verify(transactionRepo, times(2)).save(any(Transaction.class));

            verify(transactionRepo).save(argThat(transaction ->
                    getField(transaction, "user") == buyer
                            && getField(transaction, "purchase") == purchase
                            && getField(transaction, "type") == TransactionType.PURCHASE_DEBIT
                            && bigDecimalEquals(getField(transaction, "amount"), BigDecimal.valueOf(30))
                            && bigDecimalEquals(getField(transaction, "balanceAfter"), BigDecimal.valueOf(70))
                            && getField(transaction, "createdAt") != null
            ));

            verify(transactionRepo).save(argThat(transaction ->
                    getField(transaction, "user") == seller
                            && getField(transaction, "purchase") == purchase
                            && getField(transaction, "type") == TransactionType.SALE_CREDIT
                            && bigDecimalEquals(getField(transaction, "amount"), BigDecimal.valueOf(30))
                            && bigDecimalEquals(getField(transaction, "balanceAfter"), BigDecimal.valueOf(30))
                            && getField(transaction, "createdAt") != null
            ));
        }
    }

    @Nested
    class AddCreditsTransactionTests {

        @Test
        void addCreditsTransaction_validAmount_savesAddCreditTransaction() {
            AppUser user = user(
                    1L,
                    "buyer1",
                    Role.BUSINESS,
                    BigDecimal.valueOf(150)
            );

            BigDecimal amount = BigDecimal.valueOf(50);
            BigDecimal balanceAfter = BigDecimal.valueOf(150);

            transactionService.AddCreditsTransaction(
                    user,
                    amount,
                    balanceAfter
            );

            verify(transactionRepo).save(argThat(transaction ->
                    getField(transaction, "user") == user
                            && getField(transaction, "purchase") == null
                            && getField(transaction, "type") == TransactionType.ADD_CREDIT
                            && bigDecimalEquals(getField(transaction, "amount"), BigDecimal.valueOf(50))
                            && bigDecimalEquals(getField(transaction, "balanceAfter"), BigDecimal.valueOf(150))
                            && getField(transaction, "createdAt") != null
            ));

            verifyNoMoreInteractions(transactionRepo);
        }
    }

    private AppUser user(
            Long id,
            String username,
            Role role,
            BigDecimal credits
    ) {
        AppUser user = new AppUser(
                username,
                username + "@example.com",
                "password123",
                credits,
                role,
                Instant.now(),
                Instant.now()
        );

        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }

    private Object getField(Object target, String fieldName) {
        return ReflectionTestUtils.getField(target, fieldName);
    }

    private boolean bigDecimalEquals(Object actual, BigDecimal expected) {
        if (!(actual instanceof BigDecimal actualBigDecimal)) {
            return false;
        }

        return actualBigDecimal.compareTo(expected) == 0;
    }
}