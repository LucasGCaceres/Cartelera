package com.ezeiza.cartelera.dto.display;

public record PlantDisplayPublishedResponse(
        Long id,
        String plantCode,
        Long userId,
        String responsibleName,
        String responsiblePosition,
        String plantName,
        String mainTitle,
        String publishedAt
) {
}