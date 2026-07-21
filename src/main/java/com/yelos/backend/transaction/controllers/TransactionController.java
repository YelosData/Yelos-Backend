package com.yelos.backend.transaction.controllers;

import com.yelos.backend.transaction.models.TransactionResponse;
import com.yelos.backend.transaction.services.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/user/{userId}")
    public Page<TransactionResponse> getTransactionsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return transactionService.getTransactionsByUserId(
                userId,
                PageRequest.of(page, size)
        );
    }
}