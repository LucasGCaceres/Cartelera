package com.ezeiza.cartelera.dto.plant;

public record UpdatePlantRequest(
        String name,
        String displayName,
        String displayTitle,
        Integer sortOrder
) {
}