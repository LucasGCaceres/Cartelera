package com.ezeiza.cartelera.dto.member;

public record UpdatePlantMemberRequest(
        String position,
        Boolean active
) {
}