package com.yelos.backend.purchase.repo;

import com.yelos.backend.datapost.models.Genre;
import com.yelos.backend.purchase.models.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface PurchaseRepo extends JpaRepository<Purchase, Long> {
    boolean existsByBuyerIdAndPostId(Long buyerId, Long postId);
    List<Purchase> findByBuyerId(Long buyerId);

    @Query("""
        SELECT p
        FROM Purchase p
        WHERE p.buyer.id = :buyerId
        AND (:genre IS NULL OR p.genre = :genre)
    """)
    Page<Purchase> findByBuyerIdAndGenre(
            @Param("buyerId") Long buyerId,
            @Param("genre") Genre genre,
            Pageable pageable
    );

    Optional<Purchase> findByIdAndBuyerId(Long id, Long buyerId);
}
