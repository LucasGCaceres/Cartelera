package com.ezeiza.cartelera.dto.user;

public record PlantUserResponse(
        Long userId,
        String username,
        String corporateEmail,
        String fullName,
        Boolean active,
        Boolean platformAdmin,
        String roleInPlant,
        Long userPlantRoleId
) {
}