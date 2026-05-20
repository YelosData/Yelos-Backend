package com.yelos.backend.credittransaction.dto;

import java.math.BigDecimal;

public record AddCreditsRequest(
        Long userId,
        BigDecimal credits
) {
}
