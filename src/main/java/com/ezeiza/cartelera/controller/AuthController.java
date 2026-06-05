package com.ezeiza.cartelera.controller;

import com.ezeiza.cartelera.dto.AuthUserResponse;
import com.ezeiza.cartelera.dto.LoginRequest;
import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.ezeiza.cartelera.dto.RegisterRequest;
import com.ezeiza.cartelera.service.UserManagementService;
import org.springframework.security.core.AuthenticationException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final UserManagementService userManagementService;

    public AuthController(AuthenticationManager authenticationManager,
                          AppUserRepository appUserRepository, UserManagementService userManagementService) {
        this.authenticationManager = authenticationManager;
        this.appUserRepository = appUserRepository;
        this.userManagementService = userManagementService;
    }

    @PostMapping("/login")
    public AuthUserResponse login(@RequestBody LoginRequest request,
                                  HttpServletRequest httpServletRequest) {

        AppUser existingUser = appUserRepository.findByUsername(request.username())
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
                            request.username(),
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

        AppUser appUser = appUserRepository.findByUsernameAndActiveTrue(request.username())
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
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null ||
                authentication.getName() == null ||
                "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        AppUser appUser = appUserRepository.findByUsernameAndActiveTrue(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return toAuthUserResponse(appUser);
    }

    @PostMapping("/register")
    public AuthUserResponse register(@RequestBody RegisterRequest request) {
        AppUser appUser = userManagementService.registerReaderUser(
                request.username(),
                request.password(),
                request.fullName()
        );

        return toAuthUserResponse(appUser);
    }

    private AuthUserResponse toAuthUserResponse(AppUser appUser) {
        return new AuthUserResponse(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getFullName(),
                appUser.getRole().name()
        );
    }
}