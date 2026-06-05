package com.ezeiza.cartelera.dto;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String role,
        Boolean active,
        String createdAt,
        String updatedAt
) {
}
