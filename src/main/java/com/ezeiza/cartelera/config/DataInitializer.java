package com.ezeiza.cartelera.config;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.UserRole;
import com.ezeiza.cartelera.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository appUserRepository,
                           PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfNotExists(
                "admin",
                "admin",
                "Administrador del sistema",
                UserRole.ADMIN
        );

        createUserIfNotExists(
                "operador",
                "operador",
                "Usuario operador",
                UserRole.OPERADOR
        );
    }

    private void createUserIfNotExists(String username,
                                       String password,
                                       String fullName,
                                       UserRole role) {
        if (appUserRepository.existsByUsername(username)) {
            return;
        }

        AppUser user = new AppUser(
                username,
                passwordEncoder.encode(password),
                fullName,
                role
        );

        appUserRepository.save(user);
    }
}