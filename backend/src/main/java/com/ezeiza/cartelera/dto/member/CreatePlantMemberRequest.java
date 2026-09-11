package com.ezeiza.cartelera.dto.member;

public record CreatePlantMemberRequest(
        Long userId,
        String position
) {
}