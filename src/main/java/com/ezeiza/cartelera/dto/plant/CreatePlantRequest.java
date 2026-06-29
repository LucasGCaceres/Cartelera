package com.ezeiza.cartelera.dto.plant;

public record CreatePlantRequest(
        String code,
        String name,
        String displayName,
        String displayTitle,
        Integer sortOrder
) {
}