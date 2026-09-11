package com.ezeiza.cartelera.service.user;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantRole;
import com.ezeiza.cartelera.entity.UserPlantRole;
import com.ezeiza.cartelera.repository.AppUserRepository;
import com.ezeiza.cartelera.repository.UserPlantRoleRepository;
import com.ezeiza.cartelera.service.audit.AuditService;
import com.ezeiza.cartelera.service.plant.PlantPermissionService;
import com.ezeiza.cartelera.service.plant.PlantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserPlantRoleService {

    private final UserPlantRoleRepository userPlantRoleRepository;
    private final AppUserRepository appUserRepository;
    private final PlantService plantService;
    private final PlantPermissionService plantPermissionService;
    private final AuditService auditService;

    public UserPlantRoleService(UserPlantRoleRepository userPlantRoleRepository,
                                AppUserRepository appUserRepository,
                                PlantService plantService,
                                PlantPermissionService plantPermissionService,
                                AuditService auditService) {
        this.userPlantRoleRepository = userPlantRoleRepository;
        this.appUserRepository = appUserRepository;
        this.plantService = plantService;
        this.plantPermissionService = plantPermissionService;
        this.auditService = auditService;
    }

    public List<UserPlantRole> findRolesByPlant(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        return userPlantRoleRepository.findByPlantAndActiveTrue(plant);
    }

    public List<UserPlantRole> findRolesByUser(Long userId) {
        AppUser user = getUserOrThrow(userId);

        AppUser currentUser = plantPermissionService.getCurrentUserOrThrow();

        if (!currentUser.isPlatformAdmin() && !currentUser.getId().equals(user.getId())) {
            throw new IllegalArgumentException("No tenés permiso para consultar roles de este usuario");
        }

        return userPlantRoleRepository.findByUserAndActiveTrue(user);
    }

    @Transactional
    public UserPlantRole assignRole(String plantCode,
                                    Long userId,
                                    String roleValue) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        AppUser user = getUserOrThrow(userId);

        if (!user.isActive()) {
            throw new IllegalArgumentException("No se puede asignar rol a un usuario inactivo");
        }

        if (!plant.isActive()) {
            throw new IllegalArgumentException("No se puede asignar rol en una planta inactiva");
        }

        PlantRole newRole = parseRole(roleValue);

        return userPlantRoleRepository.findByUserAndPlant(user, plant)
                .map(existing -> updateExistingRole(existing, newRole))
                .orElseGet(() -> {
                    UserPlantRole createdRole = createNewRole(user, plant, newRole);

                    auditService.registerForPlant(
                            plant,
                            "ASSIGN_USER_PLANT_ROLE",
                            "UserPlantRole",
                            createdRole.getId(),
                            "Se asignó rol " + createdRole.getRole().name()
                                    + " al usuario " + user.getUsername()
                                    + " en la planta " + plant.getCode(),
                            null,
                            "userId=" + user.getId()
                                    + ", plantCode=" + plant.getCode()
                                    + ", role=" + createdRole.getRole().name()
                                    + ", active=" + createdRole.isActive()
                    );

                    return createdRole;
                });
    }

    @Transactional
    public void removeRole(String plantCode,
                           Long userId) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        AppUser user = getUserOrThrow(userId);

        UserPlantRole existingRole = userPlantRoleRepository.findByUserAndPlantAndActiveTrue(user, plant)
                .orElseThrow(() -> new IllegalArgumentException("El usuario no tiene rol activo en esta planta"));

        if (existingRole.getRole() == PlantRole.ADMIN) {
            validateNotRemovingLastActivePlantAdmin(plant);
        }

        existingRole.setActive(false);

        userPlantRoleRepository.save(existingRole);

        auditService.registerForPlant(
                plant,
                "REMOVE_USER_PLANT_ROLE",
                "UserPlantRole",
                existingRole.getId(),
                "Se quitó el rol " + existingRole.getRole().name()
                        + " al usuario " + user.getUsername()
                        + " en la planta " + plant.getCode(),
                "role=" + existingRole.getRole().name() + ", active=true",
                "role=" + existingRole.getRole().name() + ", active=false"
        );
    }

    private UserPlantRole updateExistingRole(UserPlantRole existingRole,
                                             PlantRole newRole) {
        Plant plant = existingRole.getPlant();

        PlantRole oldRole = existingRole.getRole();
        boolean oldActive = existingRole.isActive();

        if (oldRole == PlantRole.ADMIN && newRole != PlantRole.ADMIN && oldActive) {
            validateNotRemovingLastActivePlantAdmin(plant);
        }

        existingRole.setRole(newRole);
        existingRole.setActive(true);

        UserPlantRole savedRole = userPlantRoleRepository.save(existingRole);

        auditService.registerForPlant(
                plant,
                oldActive ? "UPDATE_USER_PLANT_ROLE" : "ASSIGN_USER_PLANT_ROLE",
                "UserPlantRole",
                savedRole.getId(),
                "Se actualizó el rol del usuario "
                        + savedRole.getUser().getUsername()
                        + " en la planta " + plant.getCode()
                        + " de " + oldRole.name()
                        + " a " + newRole.name(),
                "role=" + oldRole.name() + ", active=" + oldActive,
                "role=" + newRole.name() + ", active=true"
        );

        return savedRole;
    }

    private UserPlantRole createNewRole(AppUser user,
                                        Plant plant,
                                        PlantRole role) {
        UserPlantRole userPlantRole = new UserPlantRole(user, plant, role);

        return userPlantRoleRepository.save(userPlantRole);
    }

    private void validateNotRemovingLastActivePlantAdmin(Plant plant) {
        long activeAdmins = userPlantRoleRepository
                .countByPlantAndRoleAndActiveTrueAndUser_ActiveTrue(plant, PlantRole.ADMIN);

        if (activeAdmins <= 1) {
            throw new IllegalArgumentException(
                    "No se puede dejar la planta " + plant.getCode() + " sin ADMIN activo"
            );
        }
    }

    private AppUser getUserOrThrow(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }

        return appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private PlantRole parseRole(String roleValue) {
        if (roleValue == null || roleValue.trim().isBlank()) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }

        try {
            return PlantRole.valueOf(roleValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Rol de planta inválido: " + roleValue);
        }
    }
}