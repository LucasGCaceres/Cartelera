package com.ezeiza.cartelera.dto;

public record AuditLogResponse(
        Long id,
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