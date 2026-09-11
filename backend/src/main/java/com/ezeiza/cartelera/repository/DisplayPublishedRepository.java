package com.ezeiza.cartelera.repository;

import com.ezeiza.cartelera.entity.DisplayPublished;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisplayPublishedRepository extends JpaRepository<DisplayPublished, Long> {

    Optional<DisplayPublished> findTopByPlant_CodeOrderByPublishedAtDesc(String plantCode);

    Optional<DisplayPublished> findTopByPlant_CodeOrderByIdDesc(String plantCode);
}