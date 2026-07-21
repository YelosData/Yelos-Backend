package com.yelos.backend.transaction.services;

import com.yelos.backend.purchase.models.Purchase;
import com.yelos.backend.transaction.models.TransactionResponse;
import com.yelos.backend.transaction.models.Transaction;
import com.yelos.backend.transaction.models.TransactionType;
import com.yelos.backend.transaction.repo.TransactionRepo;
import com.yelos.backend.user.models.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class TransactionService {
    private final TransactionRepo transactionRepo;

    public TransactionService(TransactionRepo transactionRepo) {
        this.transactionRepo = transactionRepo;
    }

    public void PurchaseTransaction(
            AppUser buyer,
            AppUser seller,
            Purchase purchase,
            BigDecimal amount
    ) {
        Transaction buyerTransaction = new Transaction(
                buyer,
                purchase,
                TransactionType.PURCHASE_DEBIT,
                amount,
                buyer.getCredits(),
                Instant.now()
        );

        Transaction sellerTransaction = new Transaction(
                seller,
                purchase,
                TransactionType.SALE_CREDIT,
                amount,
                seller.getCredits(),
                Instant.now()
        );

        transactionRepo.save(buyerTransaction);
        transactionRepo.save(sellerTransaction);
    }

    public void AddCreditsTransaction(
            AppUser user,
            BigDecimal amount,
            BigDecimal balanceAfter
    ) {
        Transaction addTransaction = new Transaction(
                user,
                null,
                TransactionType.ADD_CREDIT,
                amount,
                balanceAfter,
                Instant.now()
        );

        transactionRepo.save(addTransaction);
    }

    public Page<TransactionResponse> getTransactionsByUserId(
            Long userId,
            Pageable pageable
    ) {
        return transactionRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getUser().getId(),
                transaction.getPurchase() == null ? null : transaction.getPurchase().getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getCreatedAt()
        );
    }
}