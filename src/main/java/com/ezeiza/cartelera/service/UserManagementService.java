package com.ezeiza.cartelera.service;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.UserRole;
import com.ezeiza.cartelera.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserManagementService {

    private static final String PRIMARY_ADMIN_USERNAME = "admin";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserManagementService(AppUserRepository appUserRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuditService auditService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public List<AppUser> findAllUsers() {
        return appUserRepository.findAll()
                .stream()
                .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                .toList();
    }

    @Transactional
    public AppUser createUser(String username,
                              String password,
                              String fullName,
                              String roleValue) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio");
        }

        if (appUserRepository.existsByUsername(username.trim())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre");
        }

        UserRole role = parseRole(roleValue);

        AppUser user = new AppUser(
                username.trim(),
                passwordEncoder.encode(password),
                fullName.trim(),
                role
        );

        AppUser savedUser = appUserRepository.save(user);

        auditService.register(
                "CREATE_USER",
                "AppUser",
                savedUser.getId(),
                "Se creó el usuario " + savedUser.getUsername() + " con rol " + savedUser.getRole().name(),
                null,
                "username=" + savedUser.getUsername() + ", role=" + savedUser.getRole().name()
        );

        return savedUser;
    }

    @Transactional
    public AppUser updateRole(Long userId, String roleValue) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserRole oldRole = user.getRole();
        UserRole newRole = parseRole(roleValue);

        if (PRIMARY_ADMIN_USERNAME.equalsIgnoreCase(user.getUsername()) && newRole != UserRole.ADMIN) {
            throw new IllegalArgumentException("No se puede cambiar el rol del administrador principal");
        }

        if (oldRole == UserRole.ADMIN && newRole != UserRole.ADMIN) {
            long activeAdmins = appUserRepository.countByRoleAndActiveTrue(UserRole.ADMIN);

            if (user.isActive() && activeAdmins <= 1) {
                throw new IllegalArgumentException("No se puede dejar el sistema sin administradores activos");
            }
        }

        user.setRole(newRole);
        AppUser savedUser = appUserRepository.save(user);

        auditService.register(
                "UPDATE_USER_ROLE",
                "AppUser",
                savedUser.getId(),
                "Se cambió el rol del usuario " + savedUser.getUsername()
                        + " de " + oldRole.name() + " a " + newRole.name(),
                "role=" + oldRole.name(),
                "role=" + newRole.name()
        );

        return savedUser;
    }

    @Transactional
    public AppUser updateStatus(Long userId, boolean active) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (PRIMARY_ADMIN_USERNAME.equalsIgnoreCase(user.getUsername()) && !active) {
            throw new IllegalArgumentException("No se puede desactivar el administrador principal");
        }

        if (user.getRole() == UserRole.ADMIN && user.isActive() && !active) {
            long activeAdmins = appUserRepository.countByRoleAndActiveTrue(UserRole.ADMIN);

            if (activeAdmins <= 1) {
                throw new IllegalArgumentException("No se puede dejar el sistema sin administradores activos");
            }
        }

        boolean oldStatus = user.isActive();

        user.setActive(active);
        AppUser savedUser = appUserRepository.save(user);

        auditService.register(
                active ? "ENABLE_USER" : "DISABLE_USER",
                "AppUser",
                savedUser.getId(),
                active
                        ? "Se activó el usuario " + savedUser.getUsername()
                        : "Se desactivó el usuario " + savedUser.getUsername(),
                "active=" + oldStatus,
                "active=" + active
        );

        return savedUser;
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("La nueva contraseña es obligatoria");
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);

        auditService.register(
                "RESET_PASSWORD",
                "AppUser",
                user.getId(),
                "Se restableció la contraseña del usuario " + user.getUsername(),
                null,
                "password=********"
        );
    }

    @Transactional
    public AppUser registerReaderUser(String username,
                                      String password,
                                      String fullName) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("El nombre completo es obligatorio");
        }

        if (appUserRepository.existsByUsername(username.trim())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre");
        }

        AppUser user = new AppUser(
                username.trim(),
                passwordEncoder.encode(password),
                fullName.trim(),
                UserRole.LECTOR
        );

        AppUser savedUser = appUserRepository.save(user);

        auditService.register(
                "REGISTER_USER",
                "AppUser",
                savedUser.getId(),
                "Se registró el usuario " + savedUser.getUsername() + " con rol LECTOR",
                null,
                "username=" + savedUser.getUsername() + ", role=LECTOR"
        );

        return savedUser;
    }

    private UserRole parseRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }

        try {
            return UserRole.valueOf(roleValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Rol inválido: " + roleValue);
        }
    }
}