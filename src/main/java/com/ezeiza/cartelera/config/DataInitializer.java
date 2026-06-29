package com.ezeiza.cartelera.config;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantRole;
import com.ezeiza.cartelera.entity.UserPlantRole;
import com.ezeiza.cartelera.repository.AppUserRepository;
import com.ezeiza.cartelera.repository.PlantRepository;
import com.ezeiza.cartelera.repository.UserPlantRoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PlantRepository plantRepository;
    private final UserPlantRoleRepository userPlantRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository appUserRepository,
                           PlantRepository plantRepository,
                           UserPlantRoleRepository userPlantRoleRepository,
                           PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.plantRepository = plantRepository;
        this.userPlantRoleRepository = userPlantRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Plant ezeiza = createPlantIfNotExists(
                "ezeiza",
                "Ezeiza",
                "PLANTA EZEIZA",
                "Responsable de planta",
                1
        );

        Plant aeroparque = createPlantIfNotExists(
                "aeroparque",
                "Aeroparque",
                "PLANTA AEROPARQUE",
                "Responsable de planta",
                2
        );

        Plant theProLaundry = createPlantIfNotExists(
                "the-pro-laundry",
                "The Pro Laundry",
                "THE PRO LAUNDRY",
                "Responsable de planta",
                3
        );

        AppUser admin = createAdminIfNotExists();

        assignPlantRoleIfNotExists(admin, ezeiza, PlantRole.ADMIN);
        assignPlantRoleIfNotExists(admin, aeroparque, PlantRole.ADMIN);
        assignPlantRoleIfNotExists(admin, theProLaundry, PlantRole.ADMIN);
    }

    private Plant createPlantIfNotExists(String code,
                                         String name,
                                         String displayName,
                                         String displayTitle,
                                         Integer sortOrder) {
        return plantRepository.findByCode(code)
                .orElseGet(() -> plantRepository.save(
                        new Plant(code, name, displayName, displayTitle, sortOrder)
                ));
    }

    private AppUser createAdminIfNotExists() {
        return appUserRepository.findByUsername("admin")
                .map(existing -> {
                    boolean changed = false;

                    if (!existing.isActive()) {
                        existing.setActive(true);
                        changed = true;
                    }

                    if (!existing.isPlatformAdmin()) {
                        existing.setPlatformAdmin(true);
                        changed = true;
                    }

                    if (existing.getCorporateEmail() == null || existing.getCorporateEmail().isBlank()) {
                        existing.setCorporateEmail("admin@local");
                        changed = true;
                    }

                    return changed ? appUserRepository.save(existing) : existing;
                })
                .orElseGet(() -> {
                    AppUser admin = new AppUser(
                            "admin",
                            "admin@local",
                            passwordEncoder.encode("admin"),
                            "Administrador del sistema",
                            true
                    );
                    admin.setActive(true);
                    return appUserRepository.save(admin);
                });
    }

    private void assignPlantRoleIfNotExists(AppUser user,
                                            Plant plant,
                                            PlantRole role) {
        userPlantRoleRepository.findByUserAndPlant(user, plant)
                .ifPresentOrElse(existing -> {
                    boolean changed = false;

                    if (!existing.isActive()) {
                        existing.setActive(true);
                        changed = true;
                    }

                    if (existing.getRole() != role) {
                        existing.setRole(role);
                        changed = true;
                    }

                    if (changed) {
                        userPlantRoleRepository.save(existing);
                    }
                }, () -> {
                    UserPlantRole userPlantRole = new UserPlantRole(user, plant, role);
                    userPlantRoleRepository.save(userPlantRole);
                });
    }
}