package com.ezeiza.cartelera.dto.auth;

public record AuthPlantRoleResponse(
        String code,
        String name,
        String displayName,
        String role
) {
}