package com.ezeiza.cartelera.dto;

public record LoginRequest(
        String username,
        String password
) {
}