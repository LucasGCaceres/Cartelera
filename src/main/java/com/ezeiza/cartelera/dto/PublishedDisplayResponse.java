package com.ezeiza.cartelera.dto;

public record PublishedDisplayResponse(
        Long id,
        Long personId,
        String responsibleName,
        String responsiblePosition,
        String plantName,
        String mainTitle,
        String publishedAt
) {
}