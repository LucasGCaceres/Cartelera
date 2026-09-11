package com.ezeiza.cartelera.service.auth;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.repository.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = username != null
                ? username.trim().toLowerCase()
                : null;

        AppUser appUser = appUserRepository.findByUsernameAndActiveTrue(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        return User.withUsername(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .roles(appUser.isPlatformAdmin() ? "PLATFORM_ADMIN" : "USER")
                .build();
    }
}