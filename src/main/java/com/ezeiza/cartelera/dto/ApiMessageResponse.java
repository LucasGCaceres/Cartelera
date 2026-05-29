package com.ezeiza.cartelera.dto;

public record ApiMessageResponse(
        Boolean success,
        String message
) {
}