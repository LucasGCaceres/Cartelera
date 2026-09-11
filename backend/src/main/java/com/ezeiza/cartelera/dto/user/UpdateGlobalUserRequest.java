package com.ezeiza.cartelera.dto.user;

public record UpdateGlobalUserRequest(
        String corporateEmail,
        String fullName,
        Boolean platformAdmin
) {
}