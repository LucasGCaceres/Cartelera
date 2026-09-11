package com.ezeiza.cartelera.dto.member;

public record PlantMemberResponse(
        Long id,
        String plantCode,
        Long userId,
        String username,
        String corporateEmail,
        String fullName,
        String position,
        Boolean available,
        Boolean active,
        String createdAt,
        String updatedAt
) {
}