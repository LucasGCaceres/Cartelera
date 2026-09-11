package com.ezeiza.cartelera.dto.user;

public record CreateGlobalUserRequest(
        String username,
        String corporateEmail,
        String fullName,
        String password,
        Boolean platformAdmin
) {
}