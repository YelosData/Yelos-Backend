package com.yelos.backend.purchase.controllers;

import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.purchase.dto.PurchasePostRequest;
import com.yelos.backend.purchase.dto.PurchaseResponse;
import com.yelos.backend.purchase.dto.PurchaseDownloadResponse;
import com.yelos.backend.purchase.services.PurchaseService;
import com.yelos.backend.security.AuthUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {
    private final PurchaseService purchaseService;
    public PurchaseController(PurchaseService purchaseService){
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public PurchaseResponse PostPurchase(@RequestBody PurchasePostRequest request) {
        return purchaseService.purchasePost(request);
    }

    @GetMapping("/users/{userId}/history")
    public PageResponse<PurchaseResponse> getPurchaseHistoryByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "ALL") String genre,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        return purchaseService.getPurchaseHistoryByUserId(userId, page, size, genre, sort);
    }

    @GetMapping("/users/{userId}/posts")
    public List<PurchaseResponse> getPurchasesByUserId(@PathVariable Long userId) {
        return purchaseService.getPurchasesByUserId(userId);
    }

    @GetMapping("/{purchaseId}/download")
    public PurchaseDownloadResponse downloadPurchasedFile(
            @PathVariable Long purchaseId,
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return purchaseService.generatePurchaseDownloadUrl(
                purchaseId,
                principal.userId()
        );
    }
}
