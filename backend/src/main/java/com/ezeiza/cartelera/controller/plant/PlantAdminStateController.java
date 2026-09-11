package com.ezeiza.cartelera.controller.plant;

import com.ezeiza.cartelera.dto.display.PlantDisplayPublishedResponse;
import com.ezeiza.cartelera.dto.member.PlantMemberResponse;
import com.ezeiza.cartelera.dto.plant.PlantAdminStateResponse;
import com.ezeiza.cartelera.dto.plant.PlantResponse;
import com.ezeiza.cartelera.dto.succession.SuccessionOrderResponse;
import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.DisplayPublished;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantMember;
import com.ezeiza.cartelera.entity.PlantRole;
import com.ezeiza.cartelera.entity.SuccessionOrder;
import com.ezeiza.cartelera.service.display.DisplayService;
import com.ezeiza.cartelera.service.plant.PlantPermissionService;
import com.ezeiza.cartelera.service.plant.PlantService;
import com.ezeiza.cartelera.service.succession.SuccessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/plants/{plantCode}/admin/state")
public class PlantAdminStateController {

    private final PlantService plantService;
    private final PlantPermissionService plantPermissionService;
    private final SuccessionService successionService;
    private final DisplayService displayService;

    public PlantAdminStateController(PlantService plantService,
                                     PlantPermissionService plantPermissionService,
                                     SuccessionService successionService,
                                     DisplayService displayService) {
        this.plantService = plantService;
        this.plantPermissionService = plantPermissionService;
        this.successionService = successionService;
        this.displayService = displayService;
    }

    @GetMapping
    public PlantAdminStateResponse getAdminState(@PathVariable String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantOperatorOrAdmin(plant.getCode());

        String currentUserRole = plantPermissionService
                .getCurrentUserRoleForPlant(plant.getCode())
                .map(PlantRole::name)
                .orElse("Sin permisos");

        Optional<PlantMember> currentResponsibleOptional =
                successionService.calculateCurrentResponsible(plant.getCode());

        Long currentResponsibleMemberId = currentResponsibleOptional
                .map(PlantMember::getId)
                .orElse(null);

        PlantMemberResponse currentResponsible = currentResponsibleOptional
                .map(this::toPlantMemberResponse)
                .orElse(null);

        List<SuccessionOrderResponse> successionList = successionService
                .findActiveSuccession(plant.getCode())
                .stream()
                .map(item -> toSuccessionOrderResponse(item, currentResponsibleMemberId))
                .toList();

        PlantDisplayPublishedResponse publishedDisplay = displayService
                .getLastPublishedDisplay(plant.getCode())
                .map(this::toPlantDisplayPublishedResponse)
                .orElse(null);

        return new PlantAdminStateResponse(
                toPlantResponse(plant),
                currentUserRole,
                currentResponsible,
                successionList,
                publishedDisplay
        );
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

    private SuccessionOrderResponse toSuccessionOrderResponse(SuccessionOrder item,
                                                              Long currentResponsibleMemberId) {
        PlantMember member = item.getPlantMember();

        return new SuccessionOrderResponse(
                item.getId(),
                item.getPlant() != null ? item.getPlant().getCode() : null,
                item.getOrderNumber(),
                member != null ? toPlantMemberResponse(member) : null,
                member != null
                        && currentResponsibleMemberId != null
                        && member.getId().equals(currentResponsibleMemberId),
                item.isActive(),
                item.getCreatedAt() != null ? item.getCreatedAt().toString() : null,
                item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : null
        );
    }

    private PlantMemberResponse toPlantMemberResponse(PlantMember member) {
        Plant plant = member.getPlant();
        AppUser user = member.getUser();

        return new PlantMemberResponse(
                member.getId(),
                plant != null ? plant.getCode() : null,
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : null,
                user != null ? user.getCorporateEmail() : null,
                user != null ? user.getFullName() : null,
                member.getPosition(),
                member.isAvailable(),
                member.isActive(),
                member.getCreatedAt() != null ? member.getCreatedAt().toString() : null,
                member.getUpdatedAt() != null ? member.getUpdatedAt().toString() : null
        );
    }

    private PlantDisplayPublishedResponse toPlantDisplayPublishedResponse(DisplayPublished published) {
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