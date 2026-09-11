package com.ezeiza.cartelera.dto.user;

public record GlobalUserResponse(
        Long id,
        String username,
        String corporateEmail,
        String fullName,
        Boolean active,
        Boolean platformAdmin,
        String entraObjectId,
        String entraTenantId,
        String createdAt,
        String updatedAt
) {
}