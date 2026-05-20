package com.yelos.backend.purchase.dto;

import java.math.BigDecimal;

public record PurchasePostRequest(
        Long buyerId,
        Long postId
) {
}
