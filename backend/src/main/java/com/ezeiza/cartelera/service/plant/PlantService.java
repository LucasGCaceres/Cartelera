package com.ezeiza.cartelera.service.plant;

import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.repository.PlantRepository;
import com.ezeiza.cartelera.service.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlantService {

    private final PlantRepository plantRepository;
    private final AuditService auditService;

    public PlantService(PlantRepository plantRepository,
                        AuditService auditService) {
        this.plantRepository = plantRepository;
        this.auditService = auditService;
    }

    public List<Plant> findAll() {
        return plantRepository.findAllByOrderBySortOrderAscNameAsc();
    }

    public List<Plant> findActive() {
        return plantRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    public Plant getByCode(String plantCode) {
        String normalizedCode = normalizeCode(plantCode);

        return plantRepository.findByCode(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Planta no encontrada: " + plantCode));
    }

    public Plant getActiveByCode(String plantCode) {
        String normalizedCode = normalizeCode(plantCode);

        return plantRepository.findByCodeAndActiveTrue(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Planta no encontrada o inactiva: " + plantCode));
    }

    @Transactional
    public Plant createPlant(String code,
                             String name,
                             String displayName,
                             String displayTitle,
                             Integer sortOrder) {
        String cleanCode = normalizeCode(code);
        String cleanName = cleanRequired(name, "El nombre de la planta es obligatorio");
        String cleanDisplayName = cleanOptional(displayName);
        String cleanDisplayTitle = cleanOptional(displayTitle);

        validateSlug(cleanCode);

        if (plantRepository.existsByCode(cleanCode)) {
            throw new IllegalArgumentException("Ya existe una planta con código: " + cleanCode);
        }

        if (cleanDisplayName == null || cleanDisplayName.isBlank()) {
            cleanDisplayName = cleanName;
        }

        if (cleanDisplayTitle == null || cleanDisplayTitle.isBlank()) {
            cleanDisplayTitle = "Responsable de planta";
        }

        Plant plant = new Plant(
                cleanCode,
                cleanName,
                cleanDisplayName,
                cleanDisplayTitle,
                sortOrder != null ? sortOrder : 0
        );

        Plant savedPlant = plantRepository.save(plant);

        auditService.registerGlobal(
                "CREATE_PLANT",
                "Plant",
                savedPlant.getId(),
                "Se creó la planta " + savedPlant.getDisplayName() + " (" + savedPlant.getCode() + ")",
                null,
                "code=" + savedPlant.getCode()
                        + ", name=" + savedPlant.getName()
                        + ", displayName=" + savedPlant.getDisplayName()
                        + ", active=" + savedPlant.isActive()
        );

        return savedPlant;
    }

    @Transactional
    public Plant updatePlant(String plantCode,
                             String name,
                             String displayName,
                             String displayTitle,
                             Integer sortOrder) {
        Plant plant = getByCode(plantCode);

        String oldValue = "name=" + plant.getName()
                + ", displayName=" + plant.getDisplayName()
                + ", displayTitle=" + plant.getDisplayTitle()
                + ", sortOrder=" + plant.getSortOrder();

        if (name != null) {
            plant.setName(cleanRequired(name, "El nombre de la planta es obligatorio"));
        }

        if (displayName != null) {
            String cleanDisplayName = cleanOptional(displayName);
            plant.setDisplayName(
                    cleanDisplayName != null && !cleanDisplayName.isBlank()
                            ? cleanDisplayName
                            : plant.getName()
            );
        }

        if (displayTitle != null) {
            String cleanDisplayTitle = cleanOptional(displayTitle);
            plant.setDisplayTitle(
                    cleanDisplayTitle != null && !cleanDisplayTitle.isBlank()
                            ? cleanDisplayTitle
                            : "Responsable de planta"
            );
        }

        if (sortOrder != null) {
            plant.setSortOrder(sortOrder);
        }

        Plant savedPlant = plantRepository.save(plant);

        String newValue = "name=" + savedPlant.getName()
                + ", displayName=" + savedPlant.getDisplayName()
                + ", displayTitle=" + savedPlant.getDisplayTitle()
                + ", sortOrder=" + savedPlant.getSortOrder();

        auditService.registerGlobal(
                "UPDATE_PLANT",
                "Plant",
                savedPlant.getId(),
                "Se actualizó la planta " + savedPlant.getDisplayName(),
                oldValue,
                newValue
        );

        return savedPlant;
    }

    @Transactional
    public Plant updateStatus(String plantCode, boolean active) {
        Plant plant = getByCode(plantCode);

        boolean oldStatus = plant.isActive();

        if (oldStatus == active) {
            return plant;
        }

        plant.setActive(active);

        Plant savedPlant = plantRepository.save(plant);

        auditService.registerGlobal(
                active ? "ENABLE_PLANT" : "DISABLE_PLANT",
                "Plant",
                savedPlant.getId(),
                active
                        ? "Se activó la planta " + savedPlant.getDisplayName()
                        : "Se desactivó la planta " + savedPlant.getDisplayName(),
                "active=" + oldStatus,
                "active=" + active
        );

        return savedPlant;
    }

    public String normalizeCode(String value) {
        if (value == null) {
            throw new IllegalArgumentException("El código de planta es obligatorio");
        }

        String normalized = value.trim().toLowerCase();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException("El código de planta es obligatorio");
        }

        return normalized;
    }

    public void validateSlug(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código de planta es obligatorio");
        }

        if (!code.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            throw new IllegalArgumentException(
                    "El código de planta debe ser un slug válido. Ejemplo: ezeiza, aeroparque, the-pro-laundry"
            );
        }
    }

    private String cleanRequired(String value, String errorMessage) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value.trim();
    }

    private String cleanOptional(String value) {
        return value == null ? null : value.trim();
    }
}