package com.ezeiza.cartelera.dto.plant;

public record PlantResponse(
        Long id,
        String code,
        String name,
        String displayName,
        String displayTitle,
        Boolean active,
        Integer sortOrder,
        String createdAt,
        String updatedAt
) {
}