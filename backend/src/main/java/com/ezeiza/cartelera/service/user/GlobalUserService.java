package com.ezeiza.cartelera.service.user;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.repository.AppUserRepository;
import com.ezeiza.cartelera.service.audit.AuditService;
import com.ezeiza.cartelera.service.plant.PlantPermissionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class GlobalUserService {

    private static final String PRIMARY_ADMIN_USERNAME = "admin";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlantPermissionService plantPermissionService;
    private final AuditService auditService;

    public GlobalUserService(AppUserRepository appUserRepository,
                             PasswordEncoder passwordEncoder,
                             PlantPermissionService plantPermissionService,
                             AuditService auditService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.plantPermissionService = plantPermissionService;
        this.auditService = auditService;
    }

    public List<AppUser> findAllUsers() {
        plantPermissionService.requirePlatformAdmin();

        return appUserRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(
                        AppUser::getUsername,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();
    }

    @Transactional
    public AppUser createUser(String username,
                              String corporateEmail,
                              String fullName,
                              String password,
                              Boolean platformAdmin) {
        plantPermissionService.requirePlatformAdmin();

        String cleanUsername = normalizeUsername(username);
        String cleanEmail = normalizeEmail(corporateEmail);
        String cleanFullName = cleanRequired(fullName, "El nombre completo es obligatorio");

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña temporal es obligatoria");
        }

        if (appUserRepository.existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese username");
        }

        if (appUserRepository.existsByCorporateEmail(cleanEmail)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email corporativo");
        }

        boolean isPlatformAdmin = Boolean.TRUE.equals(platformAdmin);

        AppUser user = new AppUser(
                cleanUsername,
                cleanEmail,
                passwordEncoder.encode(password),
                cleanFullName,
                isPlatformAdmin
        );

        user.setActive(true);
        user.setPlatformAdmin(isPlatformAdmin);

        AppUser savedUser = appUserRepository.save(user);

        auditService.registerGlobal(
                "CREATE_USER",
                "AppUser",
                savedUser.getId(),
                "Se creó el usuario global " + savedUser.getUsername(),
                null,
                "username=" + savedUser.getUsername()
                        + ", corporateEmail=" + savedUser.getCorporateEmail()
                        + ", platformAdmin=" + savedUser.isPlatformAdmin()
                        + ", active=" + savedUser.isActive()
        );

        return savedUser;
    }

    @Transactional
    public AppUser updateUser(Long userId,
                              String corporateEmail,
                              String fullName,
                              Boolean platformAdmin) {
        plantPermissionService.requirePlatformAdmin();

        AppUser user = getUserOrThrow(userId);

        String oldValue = "corporateEmail=" + user.getCorporateEmail()
                + ", fullName=" + user.getFullName()
                + ", platformAdmin=" + user.isPlatformAdmin();

        if (corporateEmail != null) {
            String cleanEmail = normalizeEmail(corporateEmail);

            appUserRepository.findByCorporateEmail(cleanEmail)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Ya existe otro usuario con ese email corporativo");
                    });

            user.setCorporateEmail(cleanEmail);
        }

        if (fullName != null) {
            user.setFullName(cleanRequired(fullName, "El nombre completo es obligatorio"));
        }

        if (platformAdmin != null) {
            boolean newPlatformAdmin = Boolean.TRUE.equals(platformAdmin);

            if (isPrimaryAdmin(user) && !newPlatformAdmin) {
                throw new IllegalArgumentException("No se puede quitar platformAdmin al administrador principal");
            }

            if (user.isPlatformAdmin() && !newPlatformAdmin) {
                validateNotRemovingLastActivePlatformAdmin(user);
            }

            user.setPlatformAdmin(newPlatformAdmin);
        }

        AppUser savedUser = appUserRepository.save(user);

        String newValue = "corporateEmail=" + savedUser.getCorporateEmail()
                + ", fullName=" + savedUser.getFullName()
                + ", platformAdmin=" + savedUser.isPlatformAdmin();

        auditService.registerGlobal(
                "UPDATE_USER",
                "AppUser",
                savedUser.getId(),
                "Se actualizó el usuario global " + savedUser.getUsername(),
                oldValue,
                newValue
        );

        return savedUser;
    }

    @Transactional
    public AppUser updateStatus(Long userId,
                                boolean active) {
        plantPermissionService.requirePlatformAdmin();

        AppUser currentUser = plantPermissionService.getCurrentUserOrThrow();
        AppUser user = getUserOrThrow(userId);

        if (isPrimaryAdmin(user) && !active) {
            throw new IllegalArgumentException("No se puede desactivar el administrador principal");
        }

        if (currentUser.getId().equals(user.getId()) && user.isActive() && !active) {
            throw new IllegalArgumentException("No podés desactivar tu propio usuario");
        }

        if (user.isPlatformAdmin() && user.isActive() && !active) {
            long activeAdmins = appUserRepository.countByPlatformAdminTrueAndActiveTrue();

            if (activeAdmins <= 1) {
                throw new IllegalArgumentException("No se puede dejar el sistema sin platformAdmin activo");
            }
        }

        boolean oldStatus = user.isActive();

        user.setActive(active);

        AppUser savedUser = appUserRepository.save(user);

        auditService.registerGlobal(
                active ? "ENABLE_USER" : "DISABLE_USER",
                "AppUser",
                savedUser.getId(),
                active
                        ? "Se activó el usuario global " + savedUser.getUsername()
                        : "Se desactivó el usuario global " + savedUser.getUsername(),
                "active=" + oldStatus,
                "active=" + active
        );

        return savedUser;
    }

    private AppUser getUserOrThrow(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }

        return appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private void validateNotRemovingLastActivePlatformAdmin(AppUser userBeingUpdated) {
        long activePlatformAdmins = appUserRepository.countByPlatformAdminTrueAndActiveTrue();

        if (userBeingUpdated.isActive() && activePlatformAdmins <= 1) {
            throw new IllegalArgumentException("No se puede dejar el sistema sin platformAdmin activo");
        }
    }

    private boolean isPrimaryAdmin(AppUser user) {
        return user != null
                && user.getUsername() != null
                && PRIMARY_ADMIN_USERNAME.equalsIgnoreCase(user.getUsername());
    }

    private String normalizeUsername(String value) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("El username es obligatorio");
        }

        return value.trim().toLowerCase();
    }

    private String normalizeEmail(String value) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("El email corporativo es obligatorio");
        }

        return value.trim().toLowerCase();
    }

    private String cleanRequired(String value, String errorMessage) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value.trim();
    }
}