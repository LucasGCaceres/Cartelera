package com.ezeiza.cartelera.dto.succession;

import com.ezeiza.cartelera.dto.member.PlantMemberResponse;

public record SuccessionOrderResponse(
        Long id,
        String plantCode,
        Integer orderNumber,
        PlantMemberResponse member,
        Boolean currentResponsible,
        Boolean active,
        String createdAt,
        String updatedAt
) {
}