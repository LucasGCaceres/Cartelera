package com.ezeiza.cartelera.repository;

import com.ezeiza.cartelera.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ezeiza.cartelera.entity.UserRole;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameAndActiveTrue(String username);

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRoleAndActiveTrue(UserRole role);
}