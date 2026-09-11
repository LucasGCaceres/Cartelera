package com.ezeiza.cartelera.controller.user;

import com.ezeiza.cartelera.dto.user.PlantRoleRequest;
import com.ezeiza.cartelera.dto.user.PlantUserResponse;
import com.ezeiza.cartelera.dto.user.UserPlantRoleResponse;
import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.UserPlantRole;
import com.ezeiza.cartelera.repository.AppUserRepository;
import com.ezeiza.cartelera.repository.UserPlantRoleRepository;
import com.ezeiza.cartelera.service.plant.PlantPermissionService;
import com.ezeiza.cartelera.service.plant.PlantService;
import com.ezeiza.cartelera.service.user.UserPlantRoleService;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/plants/{plantCode}/users")
public class PlantUserRoleController {

    private final AppUserRepository appUserRepository;
    private final UserPlantRoleRepository userPlantRoleRepository;
    private final UserPlantRoleService userPlantRoleService;
    private final PlantService plantService;
    private final PlantPermissionService plantPermissionService;

    public PlantUserRoleController(AppUserRepository appUserRepository,
                                   UserPlantRoleRepository userPlantRoleRepository,
                                   UserPlantRoleService userPlantRoleService,
                                   PlantService plantService,
                                   PlantPermissionService plantPermissionService) {
        this.appUserRepository = appUserRepository;
        this.userPlantRoleRepository = userPlantRoleRepository;
        this.userPlantRoleService = userPlantRoleService;
        this.plantService = plantService;
        this.plantPermissionService = plantPermissionService;
    }

    @GetMapping
    public List<PlantUserResponse> getUsersForPlant(@PathVariable String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        return appUserRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(
                        AppUser::getFullName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .map(user -> toPlantUserResponse(user, plant))
                .toList();
    }

    @PutMapping("/{userId}/role")
    public UserPlantRoleResponse assignRole(@PathVariable String plantCode,
                                            @PathVariable Long userId,
                                            @RequestBody PlantRoleRequest request) {
        UserPlantRole userPlantRole = userPlantRoleService.assignRole(
                plantCode,
                userId,
                request.role()
        );

        return toUserPlantRoleResponse(userPlantRole);
    }

    @DeleteMapping("/{userId}/role")
    public void removeRole(@PathVariable String plantCode,
                           @PathVariable Long userId) {
        userPlantRoleService.removeRole(plantCode, userId);
    }

    private PlantUserResponse toPlantUserResponse(AppUser user, Plant plant) {
        Optional<UserPlantRole> userPlantRoleOptional = userPlantRoleRepository
                .findByUserAndPlantAndActiveTrue(user, plant);

        String roleInPlant = userPlantRoleOptional
                .map(userPlantRole -> userPlantRole.getRole().name())
                .orElse("Sin permisos");

        Long userPlantRoleId = userPlantRoleOptional
                .map(UserPlantRole::getId)
                .orElse(null);

        return new PlantUserResponse(
                user.getId(),
                user.getUsername(),
                user.getCorporateEmail(),
                user.getFullName(),
                user.isActive(),
                user.isPlatformAdmin(),
                roleInPlant,
                userPlantRoleId
        );
    }

    private UserPlantRoleResponse toUserPlantRoleResponse(UserPlantRole userPlantRole) {
        AppUser user = userPlantRole.getUser();
        Plant plant = userPlantRole.getPlant();

        return new UserPlantRoleResponse(
                userPlantRole.getId(),
                user.getId(),
                user.getUsername(),
                user.getCorporateEmail(),
                user.getFullName(),
                plant.getCode(),
                plant.getDisplayName(),
                userPlantRole.getRole().name(),
                userPlantRole.isActive(),
                userPlantRole.getCreatedAt() != null ? userPlantRole.getCreatedAt().toString() : null,
                userPlantRole.getUpdatedAt() != null ? userPlantRole.getUpdatedAt().toString() : null
        );
    }
}