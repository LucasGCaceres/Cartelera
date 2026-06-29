package com.ezeiza.cartelera.controller.audit;

import com.ezeiza.cartelera.dto.audit.AuditLogEntryResponse;
import com.ezeiza.cartelera.entity.AuditLog;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.service.audit.AuditService;
import com.ezeiza.cartelera.service.plant.PlantPermissionService;
import com.ezeiza.cartelera.service.plant.PlantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

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

    @GetMapping("/api/plants/{plantCode}/audit-logs")
    public List<AuditLogEntryResponse> getPlantAuditLogs(@PathVariable String plantCode,
                                                         @RequestParam(required = false) String action,
                                                         @RequestParam(required = false) String username,
                                                         @RequestParam(required = false) String text,
                                                         @RequestParam(required = false) String entityName,
                                                         @RequestParam(required = false) String from,
                                                         @RequestParam(required = false) String to,
                                                         @RequestParam(defaultValue = "0") Integer page,
                                                         @RequestParam(defaultValue = "100") Integer size) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        return filterAndPage(
                auditService.findLatestByPlantCode(plant.getCode()),
                action,
                username,
                text,
                entityName,
                from,
                to,
                page,
                size
        )
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/api/audit-logs/global")
    public List<AuditLogEntryResponse> getGlobalAuditLogs(@RequestParam(required = false) String action,
                                                          @RequestParam(required = false) String username,
                                                          @RequestParam(required = false) String text,
                                                          @RequestParam(required = false) String entityName,
                                                          @RequestParam(required = false) String from,
                                                          @RequestParam(required = false) String to,
                                                          @RequestParam(defaultValue = "0") Integer page,
                                                          @RequestParam(defaultValue = "100") Integer size) {
        plantPermissionService.requirePlatformAdmin();

        return filterAndPage(
                auditService.findLatestGlobal(),
                action,
                username,
                text,
                entityName,
                from,
                to,
                page,
                size
        )
                .map(this::toResponse)
                .toList();
    }

    private Stream<AuditLog> filterAndPage(List<AuditLog> logs,
                                           String action,
                                           String username,
                                           String text,
                                           String entityName,
                                           String from,
                                           String to,
                                           Integer page,
                                           Integer size) {
        LocalDateTime fromDate = parseDateTimeOrNull(from);
        LocalDateTime toDate = parseDateTimeOrNull(to);

        int safePage = page != null && page >= 0 ? page : 0;
        int safeSize = size != null && size > 0 ? Math.min(size, 200) : 100;

        return logs.stream()
                .filter(log -> matches(action, log.getAction()))
                .filter(log -> matches(username, log.getUsername()))
                .filter(log -> matches(entityName, log.getEntityName()))
                .filter(log -> matchesText(text, log))
                .filter(log -> fromDate == null || log.getCreatedAt() == null || !log.getCreatedAt().isBefore(fromDate))
                .filter(log -> toDate == null || log.getCreatedAt() == null || !log.getCreatedAt().isAfter(toDate))
                .skip((long) safePage * safeSize)
                .limit(safeSize);
    }

    private boolean matches(String expected, String actual) {
        if (expected == null || expected.trim().isBlank()) {
            return true;
        }

        if (actual == null) {
            return false;
        }

        return actual.toLowerCase(Locale.ROOT)
                .contains(expected.trim().toLowerCase(Locale.ROOT));
    }

    private boolean matchesText(String text, AuditLog log) {
        if (text == null || text.trim().isBlank()) {
            return true;
        }

        String normalizedText = text.trim().toLowerCase(Locale.ROOT);

        return containsIgnoreCase(log.getDescription(), normalizedText)
                || containsIgnoreCase(log.getOldValue(), normalizedText)
                || containsIgnoreCase(log.getNewValue(), normalizedText)
                || containsIgnoreCase(log.getAction(), normalizedText)
                || containsIgnoreCase(log.getEntityName(), normalizedText)
                || containsIgnoreCase(log.getUsername(), normalizedText);
    }

    private boolean containsIgnoreCase(String value, String normalizedText) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(normalizedText);
    }

    private LocalDateTime parseDateTimeOrNull(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Formato de fecha inválido. Usar ISO local, ejemplo: 2026-06-25T10:30:00"
            );
        }
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