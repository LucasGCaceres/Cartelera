package com.ezeiza.cartelera.dto.auth;

public record LoginRequest(
        String username,
        String password
) {
}