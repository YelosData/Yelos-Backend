package com.yelos.backend.datapost.repo;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.DataPostStatus;
import com.yelos.backend.datapost.models.Genre;


public interface DataPostRepo extends JpaRepository<DataPost, Long> {
    public Page<DataPost> findByUserId(Long userId, Pageable pageable);
    public Page<DataPost> findByGenre(Genre genre, Pageable pageable);

    Page<DataPost> findByUserIdAndStatusNot(
            Long userId,
            DataPostStatus status,
            Pageable pageable
    );

    Page<DataPost> findByStatus(
            DataPostStatus status,
            Pageable pageable
    );

    List<DataPost> findByStatus(DataPostStatus status);


    Page<DataPost> findByGenreAndStatus(
            Genre genre,
            DataPostStatus status,
            Pageable pageable
    );

    Page<DataPost> findByStatusAndAdminReviewedFalse(
            DataPostStatus status,
            Pageable pageable
    );
}
