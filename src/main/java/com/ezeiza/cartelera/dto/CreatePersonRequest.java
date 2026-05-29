package com.ezeiza.cartelera.dto;

public record CreatePersonRequest(
        String firstName,
        String lastName,
        String position
) {
}