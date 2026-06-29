package com.ezeiza.cartelera.repository;

import com.ezeiza.cartelera.entity.Plant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantRepository extends JpaRepository<Plant, Long> {

    Optional<Plant> findByCode(String code);

    Optional<Plant> findByCodeAndActiveTrue(String code);

    boolean existsByCode(String code);

    List<Plant> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<Plant> findAllByOrderBySortOrderAscNameAsc();
}