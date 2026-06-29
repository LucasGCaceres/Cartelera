package com.ezeiza.cartelera.controller.display;

import com.ezeiza.cartelera.dto.display.PlantDisplayPublishedResponse;
import com.ezeiza.cartelera.entity.DisplayPublished;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.service.display.DisplayService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plants/{plantCode}/display")
public class PlantDisplayController {

    private final DisplayService displayService;

    public PlantDisplayController(DisplayService displayService) {
        this.displayService = displayService;
    }

    @PostMapping("/publish")
    public PlantDisplayPublishedResponse publishDisplay(@PathVariable String plantCode) {
        DisplayPublished published = displayService.publishCurrentDisplay(plantCode);

        return toResponse(published);
    }

    @GetMapping("/published")
    public PlantDisplayPublishedResponse getPublishedDisplay(@PathVariable String plantCode) {
        return displayService.getLastPublishedDisplay(plantCode)
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