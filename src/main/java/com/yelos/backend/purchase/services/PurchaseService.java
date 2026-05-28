package com.yelos.backend.purchase.services;

import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.datapost.repo.DataPostRepo;
import com.yelos.backend.globaldto.PageResponse;
import com.yelos.backend.purchase.dto.PurchasePostRequest;
import com.yelos.backend.purchase.dto.PurchaseResponse;
import com.yelos.backend.purchase.models.Purchase;
import com.yelos.backend.purchase.repo.PurchaseRepo;
import com.yelos.backend.transaction.services.TransactionService;
import com.yelos.backend.user.models.Role;
import com.yelos.backend.user.models.AppUser;
import com.yelos.backend.user.repo.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class PurchaseService {
    private final PurchaseRepo purchaseRepo;
    private final DataPostRepo dataPostRepo;
    private final UserRepo userRepo;
    private final TransactionService transactionService;
    public PurchaseService(PurchaseRepo purchaseRepo, DataPostRepo dataPostRepo, UserRepo userRepo, TransactionService transactionService) {
        this.purchaseRepo = purchaseRepo;
        this.dataPostRepo = dataPostRepo;
        this.userRepo = userRepo;
        this.transactionService = transactionService;
    }

    @Transactional
    public PurchaseResponse purchasePost(PurchasePostRequest request) {
        AppUser buyer = userRepo.findById(request.buyerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Buyer does not exist"
                ));

        DataPost post = dataPostRepo.findById(request.postId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Data post does not exist"
                ));

        AppUser seller = post.getUser();

        if (buyer.getRole() != Role.BUSINESS) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only business users can purchase posts"
            );
        }

        if (buyer.getId() == seller.getId()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot purchase your own post"
            );
        }

        if (purchaseRepo.existsByBuyerIdAndPostId(buyer.getId(), post.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already purchased this post"
            );
        }

        if (buyer.getCredits().compareTo(post.getPrice()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient credits"
            );
        }

        buyer.deductCred(post.getPrice());
        seller.addCred(post.getPrice());

        Purchase purchase = new Purchase(
                buyer,
                seller,
                post,
                post.getPrice(),
                post.getTitle(),
                post.getSummary(),
                post.getContent(),
                Instant.now()
        );

        Purchase savedPurchase = purchaseRepo.save(purchase);

        userRepo.save(buyer);
        userRepo.save(seller);

        transactionService.PurchaseTransaction(
                buyer,
                seller,
                savedPurchase,
                post.getPrice()
        );

        return toPurchaseResponse(savedPurchase);
    }
    public List<PurchaseResponse> getPurchasesByUserId(Long userId) {
        AppUser buyer = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        return purchaseRepo.findByBuyerId(buyer.getId())
                .stream()
                .map(this::toPurchaseResponse)
                .toList();
    }

    public PageResponse<PurchaseResponse> getPurchaseHistoryByUserId(
            Long userId,
            int page,
            int size,
            String genre,
            String sort
    ) {
        AppUser buyer = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        if (buyer.getRole() != Role.BUSINESS) {
            throw new RuntimeException("Only business users can view purchases");
        }

        Pageable pageable = PageRequest.of(page, size, getPurchaseSort(sort));

        Genre genreFilter = null;

        if (genre != null && !genre.equalsIgnoreCase("ALL")) {
            genreFilter = Genre.valueOf(genre.toUpperCase());
        }

        Page<Purchase> purchasePage = purchaseRepo.findByBuyerIdAndGenre(
                buyer.getId(),
                genreFilter,
                pageable
        );

        Page<PurchaseResponse> responsePage = purchasePage.map(this::toPurchaseResponse);

        return new PageResponse<>(
                responsePage.getContent(),
                responsePage.getNumber(),
                responsePage.getSize(),
                responsePage.getTotalElements(),
                responsePage.getTotalPages(),
                responsePage.isLast()
        );
    }

    private Sort getPurchaseSort(String sort) {
        return switch (sort) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "purchasedAt");
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "priceSnapshot");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "priceSnapshot");
            case "newest" -> Sort.by(Sort.Direction.DESC, "purchasedAt");
            default -> Sort.by(Sort.Direction.DESC, "purchasedAt");
        };
    }

    private PurchaseResponse toPurchaseResponse(Purchase purchase) {
        return new PurchaseResponse(
                purchase.getId(),
                purchase.getBuyer().getId(),
                purchase.getSeller().getId(),
                purchase.getDataPost().getId(),
                purchase.getPricePaid(),
                purchase.getBuyer().getCredits(),
                purchase.getTitleSnapshot(),
                purchase.getSummarySnapshot(),
                purchase.getContentSnapshot(),
                purchase.getPurchasedAt()
        );
    }
}
