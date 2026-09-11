package com.ezeiza.cartelera.dto.auth;

import java.util.List;

public record AuthUserResponse(
        Long id,
        String username,
        String corporateEmail,
        String fullName,
        Boolean active,
        Boolean platformAdmin,
        List<AuthPlantRoleResponse> plants
) {
}
