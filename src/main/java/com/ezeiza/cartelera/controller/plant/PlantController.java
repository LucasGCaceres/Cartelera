package com.ezeiza.cartelera.controller.plant;

import com.ezeiza.cartelera.dto.plant.CreatePlantRequest;
import com.ezeiza.cartelera.dto.plant.MyPlantResponse;
import com.ezeiza.cartelera.dto.plant.PlantResponse;
import com.ezeiza.cartelera.dto.plant.UpdatePlantRequest;
import com.ezeiza.cartelera.dto.plant.UpdatePlantStatusRequest;
import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.repository.UserPlantRoleRepository;
import com.ezeiza.cartelera.service.plant.PlantPermissionService;
import com.ezeiza.cartelera.service.plant.PlantService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plants")
public class PlantController {

    private final PlantService plantService;
    private final PlantPermissionService plantPermissionService;
    private final UserPlantRoleRepository userPlantRoleRepository;

    public PlantController(PlantService plantService,
                           PlantPermissionService plantPermissionService,
                           UserPlantRoleRepository userPlantRoleRepository) {
        this.plantService = plantService;
        this.plantPermissionService = plantPermissionService;
        this.userPlantRoleRepository = userPlantRoleRepository;
    }

    @GetMapping("/my")
    public List<MyPlantResponse> getMyPlants() {
        AppUser currentUser = plantPermissionService.getCurrentUserOrThrow();

        if (currentUser.isPlatformAdmin()) {
            return plantService.findActive()
                    .stream()
                    .map(plant -> new MyPlantResponse(
                            plant.getCode(),
                            plant.getName(),
                            plant.getDisplayName(),
                            "ADMIN"
                    ))
                    .toList();
        }

        return userPlantRoleRepository.findByUserAndActiveTrue(currentUser)
                .stream()
                .filter(userPlantRole -> userPlantRole.getPlant() != null)
                .filter(userPlantRole -> userPlantRole.getPlant().isActive())
                .map(userPlantRole -> new MyPlantResponse(
                        userPlantRole.getPlant().getCode(),
                        userPlantRole.getPlant().getName(),
                        userPlantRole.getPlant().getDisplayName(),
                        userPlantRole.getRole().name()
                ))
                .toList();
    }

    @GetMapping
    public List<PlantResponse> getPlants() {
        plantPermissionService.requirePlatformAdmin();

        return plantService.findAll()
                .stream()
                .map(this::toPlantResponse)
                .toList();
    }

    @PostMapping
    public PlantResponse createPlant(@RequestBody CreatePlantRequest request) {
        plantPermissionService.requirePlatformAdmin();

        Plant plant = plantService.createPlant(
                request.code(),
                request.name(),
                request.displayName(),
                request.displayTitle(),
                request.sortOrder()
        );

        return toPlantResponse(plant);
    }

    @PatchMapping("/{plantCode}")
    public PlantResponse updatePlant(@PathVariable String plantCode,
                                     @RequestBody UpdatePlantRequest request) {
        plantPermissionService.requirePlatformAdmin();

        Plant plant = plantService.updatePlant(
                plantCode,
                request.name(),
                request.displayName(),
                request.displayTitle(),
                request.sortOrder()
        );

        return toPlantResponse(plant);
    }

    @PatchMapping("/{plantCode}/status")
    public PlantResponse updatePlantStatus(@PathVariable String plantCode,
                                           @RequestBody UpdatePlantStatusRequest request) {
        plantPermissionService.requirePlatformAdmin();

        boolean active = Boolean.TRUE.equals(request.active());

        Plant plant = plantService.updateStatus(plantCode, active);

        return toPlantResponse(plant);
    }

    private PlantResponse toPlantResponse(Plant plant) {
        return new PlantResponse(
                plant.getId(),
                plant.getCode(),
                plant.getName(),
                plant.getDisplayName(),
                plant.getDisplayTitle(),
                plant.isActive(),
                plant.getSortOrder(),
                plant.getCreatedAt() != null ? plant.getCreatedAt().toString() : null,
                plant.getUpdatedAt() != null ? plant.getUpdatedAt().toString() : null
        );
    }
}