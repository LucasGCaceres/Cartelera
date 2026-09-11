package com.ezeiza.cartelera.config;

import com.ezeiza.cartelera.service.auth.AppUserDetailsService;
import com.ezeiza.cartelera.service.auth.EntraOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcherEntry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final AppUserDetailsService appUserDetailsService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;
    private final ObjectProvider<EntraOidcUserService> entraOidcUserServiceProvider;

    public SecurityConfig(AppUserDetailsService appUserDetailsService,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
                          ObjectProvider<EntraOidcUserService> entraOidcUserServiceProvider) {
        this.appUserDetailsService = appUserDetailsService;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
        this.entraOidcUserServiceProvider = entraOidcUserServiceProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Entra ID solo esta "activo" si el perfil "entra" cargo
        // application-entra.properties con credenciales validas: eso es lo
        // unico que hace que Spring cree el bean ClientRegistrationRepository.
        // Si no esta activo, esta clase se comporta EXACTAMENTE igual que antes
        // (login local unicamente, sin ningun redirect a Microsoft).
        boolean entraEnabled = clientRegistrationRepositoryProvider.getIfAvailable() != null;

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .userDetailsService(appUserDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(
                                "/",
                                "/display",
                                "/display/**",
                                "/assets/**",
                                "/favicon.ico",
                                "/vite.svg",
                                "/icons.svg",
                                "/*.svg"
                        ).permitAll()

                        .requestMatchers("/api/auth/login").permitAll()

                        .requestMatchers("/api/public/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()

                        .requestMatchers("/app/**").authenticated()

                        .requestMatchers("/api/plants/**").authenticated()
                        .requestMatchers("/api/users/global/**").authenticated()
                        .requestMatchers("/api/audit-logs/global").authenticated()

                        .anyRequest().authenticated()
                );

        if (entraEnabled) {
            // Division de comportamiento por tipo de request:
            //  - Cualquier llamada a /api/** (incluyendo el fetch('/api/auth/me')
            //    que hace el SPA para saber si hay sesion) recibe un 401 JSON
            //    limpio, tal cual funciona hoy con el login local. Nunca se la
            //    redirige a Microsoft.
            //  - Cualquier otra navegacion del navegador a una ruta protegida
            //    (por ejemplo /app/ezeiza sin sesion) se redirige automaticamente
            //    a Microsoft para iniciar el login silencioso.
            RequestMatcher apiRequestMatcher = PathPatternRequestMatcher.withDefaults().matcher("/api/**");

            AuthenticationEntryPoint apiEntryPoint = (request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

            AuthenticationEntryPoint redirectToMicrosoftEntryPoint =
                    new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/azure");

            DelegatingAuthenticationEntryPoint delegatingEntryPoint = new DelegatingAuthenticationEntryPoint(
                    redirectToMicrosoftEntryPoint,
                    new RequestMatcherEntry<>(apiRequestMatcher, apiEntryPoint)
            );

            http
                    .exceptionHandling(exceptionHandling -> exceptionHandling
                            .authenticationEntryPoint(delegatingEntryPoint)
                    )
                    .oauth2Login(oauth2 -> oauth2
                            .userInfoEndpoint(userInfo -> userInfo
                                    .oidcUserService(entraOidcUserServiceProvider.getObject())
                            )
                    );
        }

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}