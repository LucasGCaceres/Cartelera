package com.ezeiza.cartelera.service.plant;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantRole;
import com.ezeiza.cartelera.entity.UserPlantRole;
import com.ezeiza.cartelera.repository.AppUserRepository;
import com.ezeiza.cartelera.repository.UserPlantRoleRepository;
import com.ezeiza.cartelera.service.plant.PlantService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PlantPermissionService {

    private final AppUserRepository appUserRepository;
    private final UserPlantRoleRepository userPlantRoleRepository;
    private final PlantService plantService;

    public PlantPermissionService(AppUserRepository appUserRepository,
                                  UserPlantRoleRepository userPlantRoleRepository,
                                  PlantService plantService) {
        this.appUserRepository = appUserRepository;
        this.userPlantRoleRepository = userPlantRoleRepository;
        this.plantService = plantService;
    }

    public AppUser getCurrentUserOrThrow() {
        String username = getCurrentUsername();

        return appUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado o inactivo"));
    }

    public Optional<AppUser> getCurrentUser() {
        String username = getCurrentUsernameOrNull();

        if (username == null) {
            return Optional.empty();
        }

        return appUserRepository.findByUsernameAndActiveTrue(username);
    }

    public boolean isCurrentUserPlatformAdmin() {
        return getCurrentUser()
                .map(AppUser::isPlatformAdmin)
                .orElse(false);
    }

    public boolean hasAnyPlantRole(String plantCode) {
        return hasPlantRole(plantCode, PlantRole.ADMIN)
                || hasPlantRole(plantCode, PlantRole.OPERADOR);
    }

    public boolean hasPlantAdminRole(String plantCode) {
        return hasPlantRole(plantCode, PlantRole.ADMIN);
    }

    public boolean hasPlantOperatorRole(String plantCode) {
        return hasPlantRole(plantCode, PlantRole.OPERADOR);
    }

    public boolean hasPlantRole(String plantCode, PlantRole role) {
        AppUser currentUser = getCurrentUser().orElse(null);

        if (currentUser == null) {
            return false;
        }

        if (!currentUser.isActive()) {
            return false;
        }

        if (currentUser.isPlatformAdmin()) {
            return true;
        }

        Plant plant = plantService.getActiveByCode(plantCode);

        return userPlantRoleRepository
                .findByUserAndPlantAndActiveTrue(currentUser, plant)
                .map(userPlantRole -> userPlantRole.getRole() == role)
                .orElse(false);
    }

    public Optional<PlantRole> getCurrentUserRoleForPlant(String plantCode) {
        AppUser currentUser = getCurrentUser().orElse(null);

        if (currentUser == null || !currentUser.isActive()) {
            return Optional.empty();
        }

        if (currentUser.isPlatformAdmin()) {
            return Optional.of(PlantRole.ADMIN);
        }

        Plant plant = plantService.getActiveByCode(plantCode);

        return userPlantRoleRepository.findByUserAndPlantAndActiveTrue(currentUser, plant)
                .map(UserPlantRole::getRole);
    }

    public void requirePlatformAdmin() {
        AppUser currentUser = getCurrentUserOrThrow();

        if (!currentUser.isPlatformAdmin()) {
            throw new IllegalArgumentException("Se requiere permiso de administrador de plataforma");
        }
    }

    public void requirePlantAdmin(String plantCode) {
        AppUser currentUser = getCurrentUserOrThrow();

        if (currentUser.isPlatformAdmin()) {
            return;
        }

        Plant plant = plantService.getActiveByCode(plantCode);

        boolean allowed = userPlantRoleRepository
                .findByUserAndPlantAndActiveTrue(currentUser, plant)
                .map(role -> role.getRole() == PlantRole.ADMIN)
                .orElse(false);

        if (!allowed) {
            throw new IllegalArgumentException("Se requiere permiso ADMIN en la planta " + plant.getCode());
        }
    }

    public void requirePlantOperatorOrAdmin(String plantCode) {
        AppUser currentUser = getCurrentUserOrThrow();

        if (currentUser.isPlatformAdmin()) {
            return;
        }

        Plant plant = plantService.getActiveByCode(plantCode);

        boolean allowed = userPlantRoleRepository
                .findByUserAndPlantAndActiveTrue(currentUser, plant)
                .map(role -> role.getRole() == PlantRole.ADMIN || role.getRole() == PlantRole.OPERADOR)
                .orElse(false);

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Se requiere permiso ADMIN u OPERADOR en la planta " + plant.getCode()
            );
        }
    }

    public String getCurrentUsername() {
        String username = getCurrentUsernameOrNull();

        if (username == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }

        return username;
    }

    private String getCurrentUsernameOrNull() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        String username = authentication.getName();

        if ("anonymousUser".equals(username)) {
            return null;
        }

        return username;
    }
}