package com.yelos.backend.credittransaction.services;

import com.yelos.backend.credittransaction.dto.AddCreditsRequest;
import com.yelos.backend.credittransaction.dto.CreditsReponse;
import com.yelos.backend.transaction.services.TransactionService;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.repo.UserRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CreditServices {
    private final UserRepo userRepo;
    private final TransactionService transactionService;

    public CreditServices(UserRepo userRepo, TransactionService transactionService) {
        this.userRepo = userRepo;
        this.transactionService = transactionService;
    }

    public CreditsReponse addCredits(AddCreditsRequest request) {
        Long userId = request.userId();
        BigDecimal amount = request.credits();
        System.out.println(request);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Invalid amount added");
        }
        AppUser user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.addCred(amount);
        transactionService.AddCreditsTransaction(user, amount, user.getCredits());
        userRepo.save(user);
        return new CreditsReponse(
                userId,
                user.getCredits()
        );
    }

    //Implement like withdraw later
}
