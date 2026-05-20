package com.yelos.backend.user.repo;

import com.yelos.backend.user.models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<AppUser, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<AppUser> findByUsername(String username);
}
