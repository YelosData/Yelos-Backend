package com.yelos.backend.credittransaction.controllers;

import com.yelos.backend.credittransaction.dto.AddCreditsRequest;
import com.yelos.backend.credittransaction.dto.CreditsReponse;
import com.yelos.backend.credittransaction.services.CreditServices;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/credittransaction")
public class CreditTransactionController {
    private CreditServices creditServices;
    public CreditTransactionController(CreditServices creditServices) {
        this.creditServices = creditServices;
    }

    @PostMapping("/add")
    public CreditsReponse addCreditsToUserId(@RequestBody AddCreditsRequest request) {
        return creditServices.addCredits(request);
    }
}
