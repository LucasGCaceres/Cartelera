package com.ezeiza.cartelera.controller.auth;

import com.ezeiza.cartelera.dto.auth.AuthPlantRoleResponse;
import com.ezeiza.cartelera.dto.auth.AuthUserResponse;
import com.ezeiza.cartelera.dto.auth.LoginRequest;
import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.UserPlantRole;
import com.ezeiza.cartelera.repository.AppUserRepository;
import com.ezeiza.cartelera.repository.PlantRepository;
import com.ezeiza.cartelera.repository.UserPlantRoleRepository;
import com.ezeiza.cartelera.service.auth.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final UserPlantRoleRepository userPlantRoleRepository;
    private final PlantRepository plantRepository;
    private final CurrentUserResolver currentUserResolver;

    public AuthController(AuthenticationManager authenticationManager,
                          AppUserRepository appUserRepository,
                          UserPlantRoleRepository userPlantRoleRepository,
                          PlantRepository plantRepository,
                          CurrentUserResolver currentUserResolver) {
        this.authenticationManager = authenticationManager;
        this.appUserRepository = appUserRepository;
        this.userPlantRoleRepository = userPlantRoleRepository;
        this.plantRepository = plantRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/login")
    public AuthUserResponse login(@RequestBody LoginRequest request,
                                  HttpServletRequest httpServletRequest) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La solicitud es obligatoria.");
        }

        if (request.username() == null || request.username().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario es obligatorio.");
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria.");
        }

        String username = request.username().trim().toLowerCase();

        AppUser existingUser = appUserRepository.findByUsername(username)
                .orElse(null);

        if (existingUser != null && !existingUser.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.LOCKED,
                    "El usuario está deshabilitado. Contactá a un administrador."
            );
        }

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            request.password()
                    )
            );
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario o contraseña incorrectos."
            );
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpSession session = httpServletRequest.getSession(true);

        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        AppUser appUser = appUserRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return toAuthUserResponse(appUser);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest httpServletRequest) {
        HttpSession session = httpServletRequest.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();
    }

    @GetMapping("/me")
    public AuthUserResponse me() {
        AppUser appUser = currentUserResolver.resolveCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return toAuthUserResponse(appUser);
    }

    @PostMapping("/logout-all")
    public void logoutAll(HttpServletRequest httpServletRequest) {
        logout(httpServletRequest);
    }

    private AuthUserResponse toAuthUserResponse(AppUser appUser) {
        return new AuthUserResponse(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getCorporateEmail(),
                appUser.getFullName(),
                appUser.isActive(),
                appUser.isPlatformAdmin(),
                buildPlantRoles(appUser)
        );
    }

    private List<AuthPlantRoleResponse> buildPlantRoles(AppUser appUser) {
        if (appUser.isPlatformAdmin()) {
            return plantRepository.findByActiveTrueOrderBySortOrderAscNameAsc()
                    .stream()
                    .map(plant -> new AuthPlantRoleResponse(
                            plant.getCode(),
                            plant.getName(),
                            plant.getDisplayName(),
                            "ADMIN"
                    ))
                    .toList();
        }

        return userPlantRoleRepository.findByUserAndActiveTrue(appUser)
                .stream()
                .filter(userPlantRole -> userPlantRole.getPlant() != null)
                .filter(userPlantRole -> userPlantRole.getPlant().isActive())
                .sorted(Comparator.comparingInt(this::getPlantSortOrderSafe))
                .map(this::toAuthPlantRoleResponse)
                .toList();
    }

    private int getPlantSortOrderSafe(UserPlantRole userPlantRole) {
        Plant plant = userPlantRole.getPlant();

        if (plant == null || plant.getSortOrder() == null) {
            return Integer.MAX_VALUE;
        }

        return plant.getSortOrder();
    }

    private AuthPlantRoleResponse toAuthPlantRoleResponse(UserPlantRole userPlantRole) {
        Plant plant = userPlantRole.getPlant();

        return new AuthPlantRoleResponse(
                plant.getCode(),
                plant.getName(),
                plant.getDisplayName(),
                userPlantRole.getRole().name()
        );
    }
}