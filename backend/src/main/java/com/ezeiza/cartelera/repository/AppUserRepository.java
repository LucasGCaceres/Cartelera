package com.ezeiza.cartelera.repository;

import com.ezeiza.cartelera.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameAndActiveTrue(String username);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByCorporateEmail(String corporateEmail);

    Optional<AppUser> findByCorporateEmailAndActiveTrue(String corporateEmail);

    Optional<AppUser> findByEntraObjectId(String entraObjectId);

    Optional<AppUser> findByEntraObjectIdAndActiveTrue(String entraObjectId);

    boolean existsByUsername(String username);

    boolean existsByCorporateEmail(String corporateEmail);

    long countByPlatformAdminTrueAndActiveTrue();
}