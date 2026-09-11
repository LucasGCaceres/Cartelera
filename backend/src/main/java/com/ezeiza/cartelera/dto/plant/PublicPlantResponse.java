package com.ezeiza.cartelera.dto.plant;

public record PublicPlantResponse(
        Long id,
        String code,
        String name,
        String displayName,
        String displayTitle,
        Integer sortOrder
) {
}