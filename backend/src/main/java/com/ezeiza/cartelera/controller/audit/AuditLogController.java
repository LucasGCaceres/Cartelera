package com.ezeiza.cartelera.controller.audit;

import com.ezeiza.cartelera.dto.audit.AuditLogEntryResponse;
import com.ezeiza.cartelera.dto.audit.PagedAuditLogResponse;
import com.ezeiza.cartelera.entity.AuditLog;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.service.audit.AuditService;
import com.ezeiza.cartelera.service.plant.PlantPermissionService;
import com.ezeiza.cartelera.service.plant.PlantService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class AuditLogController {

    private final AuditService auditService;
    private final PlantService plantService;
    private final PlantPermissionService plantPermissionService;

    public AuditLogController(AuditService auditService,
                              PlantService plantService,
                              PlantPermissionService plantPermissionService) {
        this.auditService = auditService;
        this.plantService = plantService;
        this.plantPermissionService = plantPermissionService;
    }

    @GetMapping("/api/audit-logs")
    public PagedAuditLogResponse getAuditLogs(@RequestParam(required = false) String plantCode,
                                              @RequestParam(required = false) String action,
                                              @RequestParam(required = false) String username,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                              @RequestParam(defaultValue = "0") Integer page,
                                              @RequestParam(defaultValue = "25") Integer size) {
        plantPermissionService.requirePlatformAdmin();
        validateDateRange(from, to);

        String normalizedPlantCode = plantCode != null && !plantCode.trim().isBlank()
                ? plantCode.trim()
                : null;

        if (normalizedPlantCode != null) {
            plantService.getActiveByCode(normalizedPlantCode);
        }

        return toPagedResponse(auditService.searchAuditLogs(
                normalizedPlantCode,
                false,
                action,
                username,
                from,
                to,
                page,
                size
        ));
    }

    @GetMapping("/api/plants/{plantCode}/audit-logs")
    public PagedAuditLogResponse getPlantAuditLogs(@PathVariable String plantCode,
                                                   @RequestParam(required = false) String action,
                                                   @RequestParam(required = false) String username,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                   @RequestParam(defaultValue = "0") Integer page,
                                                   @RequestParam(defaultValue = "25") Integer size) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());
        validateDateRange(from, to);

        return toPagedResponse(auditService.searchAuditLogs(
                plant.getCode(),
                false,
                action,
                username,
                from,
                to,
                page,
                size
        ));
    }

    @GetMapping("/api/audit-logs/global")
    public PagedAuditLogResponse getGlobalAuditLogs(@RequestParam(required = false) String action,
                                                    @RequestParam(required = false) String username,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                    @RequestParam(defaultValue = "0") Integer page,
                                                    @RequestParam(defaultValue = "25") Integer size) {
        plantPermissionService.requirePlatformAdmin();
        validateDateRange(from, to);

        return toPagedResponse(auditService.searchAuditLogs(
                null,
                true,
                action,
                username,
                from,
                to,
                page,
                size
        ));
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
    }

    private PagedAuditLogResponse toPagedResponse(Page<AuditLog> auditPage) {
        return PagedAuditLogResponse.from(auditPage.map(this::toResponse));
    }

    private AuditLogEntryResponse toResponse(AuditLog auditLog) {
        Plant plant = auditLog.getPlant();

        return new AuditLogEntryResponse(
                auditLog.getId(),
                plant != null ? plant.getCode() : null,
                plant != null ? plant.getDisplayName() : null,
                auditLog.getUsername(),
                auditLog.getAction(),
                auditLog.getEntityName(),
                auditLog.getEntityId(),
                auditLog.getDescription(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getCreatedAt() != null ? auditLog.getCreatedAt().toString() : null
        );
    }
}