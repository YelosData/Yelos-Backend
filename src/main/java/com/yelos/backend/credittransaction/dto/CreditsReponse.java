package com.yelos.backend.credittransaction.dto;

import java.math.BigDecimal;

public record CreditsReponse(
        Long userId,
        BigDecimal credits
) {
}
