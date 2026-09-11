package com.ezeiza.cartelera.service.auth;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Punto unico para resolver "quien es el AppUser autenticado ahora mismo",
 * sin importar si la autenticacion vino del login local (usuario/contrasena)
 * o de Microsoft Entra ID (OAuth2/OIDC).
 *
 * Antes de esto, tres lugares distintos del codigo (AuthController,
 * PlantPermissionService, AuditService) asumian que
 * Authentication.getName() siempre era el "username" interno. Eso deja de
 * ser cierto con Entra ID: ahi el principal es un OidcUser y lo unico
 * confiable para identificar a la persona es el claim "oid" (object id).
 */
@Service
public class CurrentUserResolver {

    private final AppUserRepository appUserRepository;

    public CurrentUserResolver(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public Optional<AppUser> resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            return Optional.empty();
        }

        if ("anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }

        if (authentication instanceof OAuth2AuthenticationToken oauth2Token
                && oauth2Token.getPrincipal() instanceof OidcUser oidcUser) {
            String objectId = oidcUser.getClaimAsString("oid");
            if (objectId == null || objectId.isBlank()) {
                return Optional.empty();
            }
            return appUserRepository.findByEntraObjectIdAndActiveTrue(objectId);
        }

        String username = authentication.getName().trim().toLowerCase();
        return appUserRepository.findByUsernameAndActiveTrue(username);
    }
}
