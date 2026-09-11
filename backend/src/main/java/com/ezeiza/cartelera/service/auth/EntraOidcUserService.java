package com.ezeiza.cartelera.service.auth;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Se ejecuta cada vez que alguien inicia sesion con Microsoft Entra ID.
 *
 * Logica de resolucion del AppUser (en este orden):
 *   1) Si ya existe un AppUser con este entraObjectId -> es esa persona,
 *      ya se logueo antes con Entra.
 *   2) Si no, pero existe un AppUser con este corporateEmail -> es un
 *      usuario "pre-provisionado" por un admin (le cargaron el mail y le
 *      asignaron rol de planta antes de que se loguee por primera vez).
 *      Se "adopta" ese registro: se le completa el entraObjectId para que
 *      la proxima vez entre directo por el paso 1.
 *   3) Si no hay ninguno de los dos -> es la primera vez que esta persona
 *      usa la app y nadie le asigno permisos todavia. Se crea un AppUser
 *      nuevo, sin roles de planta (mismo comportamiento que hoy tiene un
 *      usuario "sin permisos asignados").
 */
@Service
public class EntraOidcUserService extends OidcUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public EntraOidcUserService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String objectId = oidcUser.getClaimAsString("oid");
        String tenantId = oidcUser.getClaimAsString("tid");
        String email = firstNonBlank(oidcUser.getClaimAsString("email"), oidcUser.getClaimAsString("preferred_username"));
        String fullName = oidcUser.getClaimAsString("name");

        if (isBlank(objectId)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_token", "La cuenta de Microsoft no incluyo un identificador valido (oid).", null));
        }

        AppUser appUser = appUserRepository.findByEntraObjectId(objectId).orElse(null);

        if (appUser == null && !isBlank(email)) {
            appUser = appUserRepository.findByCorporateEmail(normalize(email)).orElse(null);
        }

        if (appUser == null) {
            appUser = new AppUser();
            String normalizedEmail = !isBlank(email) ? normalize(email) : objectId + "@sin-email.local";
            appUser.setUsername(normalizedEmail);
            appUser.setCorporateEmail(normalizedEmail);
            appUser.setFullName(!isBlank(fullName) ? fullName : normalizedEmail);
            // Password aleatoria e inutilizable: este usuario solo va a poder
            // entrar por Entra ID, nunca por el formulario de login local.
            appUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            appUser.setPlatformAdmin(false);
            appUser.setActive(true);
        }

        if (!appUser.isActive()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("access_denied", "El usuario esta deshabilitado. Contacta a un administrador.", null));
        }

        appUser.setEntraObjectId(objectId);
        appUser.setEntraTenantId(tenantId);
        if (!isBlank(fullName)) {
            appUser.setFullName(fullName);
        }

        appUserRepository.save(appUser);

        return oidcUser;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value.trim().toLowerCase();
    }

    private String firstNonBlank(String a, String b) {
        return !isBlank(a) ? a : b;
    }
}
