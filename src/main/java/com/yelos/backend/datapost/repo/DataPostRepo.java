package com.yelos.backend.datapost.repo;


import com.yelos.backend.datapost.dto.GenreReponse;
import com.yelos.backend.datapost.models.DataPost;
import com.yelos.backend.datapost.models.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DataPostRepo extends JpaRepository<DataPost, Long> {
    public Page<DataPost> findByUserId(Long userId, Pageable pageable);
    public Page<DataPost> findByGenre(Genre genre, Pageable pageable);

//    @Query("SELECT DISTINCT p.genre FROM DataPost p")
//    public List<String> findDistinctGenre();

}
