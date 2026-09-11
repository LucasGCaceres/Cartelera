package com.ezeiza.cartelera.dto;

public record CreateUserRequest(
        String username,
        String password,
        String fullName,
        String role
) {
}