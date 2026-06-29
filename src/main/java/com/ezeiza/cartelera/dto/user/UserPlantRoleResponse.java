package com.ezeiza.cartelera.dto.user;

public record UserPlantRoleResponse(
        Long id,
        Long userId,
        String username,
        String corporateEmail,
        String fullName,
        String plantCode,
        String plantName,
        String role,
        Boolean active,
        String createdAt,
        String updatedAt
) {
}