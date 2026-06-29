package com.ezeiza.cartelera.repository;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlantMemberRepository extends JpaRepository<PlantMember, Long> {

    Optional<PlantMember> findByPlantAndUser(Plant plant, AppUser user);

    Optional<PlantMember> findByPlantAndUserAndActiveTrue(Plant plant, AppUser user);

    Optional<PlantMember> findByIdAndPlant_Code(Long id, String plantCode);

    Optional<PlantMember> findByIdAndPlant_CodeAndActiveTrue(Long id, String plantCode);

    List<PlantMember> findByPlantAndActiveTrueOrderByUser_FullNameAsc(Plant plant);

    List<PlantMember> findByPlant_CodeAndActiveTrueOrderByUser_FullNameAsc(String plantCode);

    List<PlantMember> findByPlant_CodeOrderByUser_FullNameAsc(String plantCode);

    boolean existsByPlantAndUser(Plant plant, AppUser user);

    boolean existsByPlantAndUserAndActiveTrue(Plant plant, AppUser user);

    boolean existsByPlant_CodeAndUser_IdAndActiveTrue(String plantCode, Long userId);
}