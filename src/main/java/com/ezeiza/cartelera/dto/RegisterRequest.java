package com.ezeiza.cartelera.dto;

public record RegisterRequest(
        String username,
        String password,
        String fullName
) {
}