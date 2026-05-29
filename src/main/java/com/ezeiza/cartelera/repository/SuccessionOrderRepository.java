package com.ezeiza.cartelera.repository;

import com.ezeiza.cartelera.entity.SuccessionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SuccessionOrderRepository extends JpaRepository<SuccessionOrder, Long> {

    List<SuccessionOrder> findByActiveTrueOrderByOrderNumberAsc();

    Optional<SuccessionOrder> findByPersonId(Long personId);

    Optional<SuccessionOrder> findByPersonIdAndActiveTrue(Long personId);

    Optional<SuccessionOrder> findByOrderNumberAndActiveTrue(Integer orderNumber);

    @Query("select coalesce(max(s.orderNumber), 0) from SuccessionOrder s where s.active = true")
    Integer findMaxActiveOrderNumber();

    @Query("select coalesce(max(s.orderNumber), 0) from SuccessionOrder s")
    Integer findMaxOrderNumber();
}