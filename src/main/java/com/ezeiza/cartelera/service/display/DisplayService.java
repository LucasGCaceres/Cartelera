package com.ezeiza.cartelera.service.display;

import com.ezeiza.cartelera.entity.DisplayPublished;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantMember;
import com.ezeiza.cartelera.repository.DisplayPublishedRepository;
import com.ezeiza.cartelera.service.audit.AuditService;
import com.ezeiza.cartelera.service.plant.PlantPermissionService;
import com.ezeiza.cartelera.service.plant.PlantService;
import com.ezeiza.cartelera.service.succession.SuccessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class DisplayService {

    private final DisplayPublishedRepository displayPublishedRepository;
    private final AuditService auditService;
    private final PlantService plantService;
    private final PlantPermissionService plantPermissionService;
    private final SuccessionService successionService;

    public DisplayService(DisplayPublishedRepository displayPublishedRepository,
                          AuditService auditService,
                          PlantService plantService,
                          PlantPermissionService plantPermissionService,
                          SuccessionService successionService) {
        this.displayPublishedRepository = displayPublishedRepository;
        this.auditService = auditService;
        this.plantService = plantService;
        this.plantPermissionService = plantPermissionService;
        this.successionService = successionService;
    }

    public Optional<PlantMember> calculateCurrentResponsible(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantOperatorOrAdmin(plant.getCode());

        return successionService.calculateCurrentResponsible(plant.getCode());
    }

    public Optional<PlantMember> calculateCurrentResponsiblePublic(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        return successionService.calculateCurrentResponsible(plant.getCode());
    }

    @Transactional
    public DisplayPublished publishCurrentDisplay(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantOperatorOrAdmin(plant.getCode());

        Optional<PlantMember> responsibleOptional =
                successionService.calculateCurrentResponsible(plant.getCode());

        DisplayPublished published;

        if (responsibleOptional.isPresent()) {
            PlantMember responsible = responsibleOptional.get();

            published = new DisplayPublished(
                    plant,
                    responsible.getUser().getId(),
                    responsible.getDisplayName(),
                    responsible.getPosition(),
                    plant.getDisplayName(),
                    plant.getDisplayTitle()
            );
        } else {
            published = new DisplayPublished(
                    plant,
                    null,
                    "Sin responsable disponible",
                    null,
                    plant.getDisplayName(),
                    plant.getDisplayTitle()
            );
        }

        DisplayPublished savedPublished = displayPublishedRepository.save(published);

        auditService.registerForPlant(
                plant,
                "PUBLISH_DISPLAY",
                "DisplayPublished",
                savedPublished.getId(),
                savedPublished.getUserId() != null
                        ? "Se publicó la cartelera de " + plant.getCode()
                          + " con responsable " + savedPublished.getResponsibleName()
                        : "Se publicó la cartelera de " + plant.getCode()
                          + " sin responsable disponible",
                null,
                "userId=" + savedPublished.getUserId()
                        + ", responsibleName=" + savedPublished.getResponsibleName()
                        + ", plantCode=" + plant.getCode()
        );

        return savedPublished;
    }

    public Optional<DisplayPublished> getLastPublishedDisplay(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantOperatorOrAdmin(plant.getCode());

        return displayPublishedRepository.findTopByPlant_CodeOrderByPublishedAtDesc(plant.getCode());
    }

    public Optional<DisplayPublished> getLastPublishedDisplayPublic(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        return displayPublishedRepository.findTopByPlant_CodeOrderByPublishedAtDesc(plant.getCode());
    }
}