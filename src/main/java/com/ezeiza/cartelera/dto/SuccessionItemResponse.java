package com.ezeiza.cartelera.dto;

public record SuccessionItemResponse(
        Long id,
        Integer orderNumber,
        PersonResponse person
) {
}