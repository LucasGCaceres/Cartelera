package com.ezeiza.cartelera.controller.display;

import com.ezeiza.cartelera.dto.display.PlantDisplayPublishedResponse;
import com.ezeiza.cartelera.entity.DisplayPublished;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.service.display.DisplayService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/plants")
public class PublicDisplayController {

    private final DisplayService displayService;

    public PublicDisplayController(DisplayService displayService) {
        this.displayService = displayService;
    }

    @GetMapping("/{plantCode}/display/published")
    public PlantDisplayPublishedResponse getPublishedDisplay(@PathVariable String plantCode) {
        return displayService.getLastPublishedDisplayPublic(plantCode)
                .map(this::toResponse)
                .orElse(null);
    }

    private PlantDisplayPublishedResponse toResponse(DisplayPublished published) {
        Plant plant = published.getPlant();

        return new PlantDisplayPublishedResponse(
                published.getId(),
                plant != null ? plant.getCode() : null,
                published.getUserId(),
                published.getResponsibleName(),
                published.getResponsiblePosition(),
                published.getPlantName(),
                published.getMainTitle(),
                published.getPublishedAt() != null ? published.getPublishedAt().toString() : null
        );
    }
}