package com.ezeiza.cartelera.repository;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantRole;
import com.ezeiza.cartelera.entity.UserPlantRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPlantRoleRepository extends JpaRepository<UserPlantRole, Long> {

    Optional<UserPlantRole> findByUserAndPlant(AppUser user, Plant plant);

    Optional<UserPlantRole> findByUserAndPlantAndActiveTrue(AppUser user, Plant plant);

    Optional<UserPlantRole> findByUser_IdAndPlant_CodeAndActiveTrue(Long userId, String plantCode);

    List<UserPlantRole> findByUserAndActiveTrue(AppUser user);

    List<UserPlantRole> findByUser_IdAndActiveTrue(Long userId);

    List<UserPlantRole> findByPlantAndActiveTrue(Plant plant);

    List<UserPlantRole> findByPlant_CodeAndActiveTrue(String plantCode);

    long countByPlantAndRoleAndActiveTrue(Plant plant, PlantRole role);

    long countByPlantAndRoleAndActiveTrueAndUser_ActiveTrue(
            Plant plant,
            PlantRole role
    );

    boolean existsByUserAndPlantAndActiveTrue(AppUser user, Plant plant);

    boolean existsByUser_IdAndPlant_CodeAndActiveTrue(Long userId, String plantCode);
}