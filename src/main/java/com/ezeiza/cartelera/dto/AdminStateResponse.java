package com.ezeiza.cartelera.dto;

import java.util.List;

public record AdminStateResponse(
        PersonResponse currentResponsible,
        List<SuccessionItemResponse> successionList
) {
}