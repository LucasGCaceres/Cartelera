package com.ezeiza.cartelera.dto.audit;

public record AuditLogEntryResponse(
        Long id,
        String plantCode,
        String plantName,
        String username,
        String action,
        String entityName,
        Long entityId,
        String description,
        String oldValue,
        String newValue,
        String createdAt
) {
}