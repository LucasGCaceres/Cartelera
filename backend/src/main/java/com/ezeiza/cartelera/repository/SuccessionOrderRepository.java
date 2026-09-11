package com.ezeiza.cartelera.repository;

import com.ezeiza.cartelera.entity.SuccessionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SuccessionOrderRepository extends JpaRepository<SuccessionOrder, Long> {

    List<SuccessionOrder> findByPlant_CodeAndActiveTrueOrderByOrderNumberAsc(String plantCode);

    List<SuccessionOrder> findByPlant_CodeOrderByOrderNumberAsc(String plantCode);

    Optional<SuccessionOrder> findByPlant_CodeAndPlantMember_Id(
            String plantCode,
            Long plantMemberId
    );

    Optional<SuccessionOrder> findByPlant_CodeAndPlantMember_IdAndActiveTrue(
            String plantCode,
            Long plantMemberId
    );

    Optional<SuccessionOrder> findByPlant_CodeAndOrderNumberAndActiveTrue(
            String plantCode,
            Integer orderNumber
    );

    boolean existsByPlant_CodeAndPlantMember_IdAndActiveTrue(
            String plantCode,
            Long plantMemberId
    );

    @Query("""
            select coalesce(max(s.orderNumber), 0)
            from SuccessionOrder s
            where s.active = true
              and s.plant.code = :plantCode
            """)
    Integer findMaxActiveOrderNumberByPlantCode(String plantCode);
}
