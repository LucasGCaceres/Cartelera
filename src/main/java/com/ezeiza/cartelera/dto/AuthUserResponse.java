package com.ezeiza.cartelera.dto;

public record AuthUserResponse(
        Long id,
        String username,
        String fullName,
        String role
) {
}