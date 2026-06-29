package com.ezeiza.cartelera.controller.plant;

import com.ezeiza.cartelera.dto.plant.PublicPlantResponse;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.service.plant.PlantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PublicPlantController {

    private final PlantService plantService;

    public PublicPlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    @GetMapping("/api/public/plants")
    public List<PublicPlantResponse> getPublicPlants() {
        return plantService.findActive()
                .stream()
                .map(this::toPublicPlantResponse)
                .toList();
    }

    private PublicPlantResponse toPublicPlantResponse(Plant plant) {
        return new PublicPlantResponse(
                plant.getId(),
                plant.getCode(),
                plant.getName(),
                plant.getDisplayName(),
                plant.getDisplayTitle(),
                plant.getSortOrder()
        );
    }
}