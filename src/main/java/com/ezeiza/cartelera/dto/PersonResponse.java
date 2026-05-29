package com.ezeiza.cartelera.dto;

public record PersonResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String position,
        Boolean available,
        Boolean active
) {
}
