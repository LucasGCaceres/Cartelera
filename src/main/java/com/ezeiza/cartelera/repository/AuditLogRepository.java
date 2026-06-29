package com.ezeiza.cartelera.repository;

import com.ezeiza.cartelera.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /*
     * Legacy/global actual.
     */
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    /*
     * Nuevo modelo por planta.
     */
    List<AuditLog> findTop100ByPlant_CodeOrderByCreatedAtDesc(String plantCode);

    /*
     * Acciones globales, plant = null.
     */
    List<AuditLog> findTop100ByPlantIsNullOrderByCreatedAtDesc();
}