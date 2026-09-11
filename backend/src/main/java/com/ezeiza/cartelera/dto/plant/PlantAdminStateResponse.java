package com.ezeiza.cartelera.dto.plant;

import com.ezeiza.cartelera.dto.display.PlantDisplayPublishedResponse;
import com.ezeiza.cartelera.dto.member.PlantMemberResponse;
import com.ezeiza.cartelera.dto.succession.SuccessionOrderResponse;

import java.util.List;

public record PlantAdminStateResponse(
        PlantResponse plant,
        String currentUserRole,
        PlantMemberResponse currentResponsible,
        List<SuccessionOrderResponse> successionList,
        PlantDisplayPublishedResponse publishedDisplay
) {
}