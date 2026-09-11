package com.ezeiza.cartelera.service.audit;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.AuditLog;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.repository.AuditLogRepository;
import com.ezeiza.cartelera.service.auth.CurrentUserResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserResolver currentUserResolver;

    public AuditService(AuditLogRepository auditLogRepository, CurrentUserResolver currentUserResolver) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /*
     * Registro legacy/global.
     * Se mantiene para que PersonService, DisplayService y UserManagementService
     * actuales sigan compilando y funcionando.
     */
    public void register(String action,
                         String entityName,
                         Long entityId,
                         String description,
                         String oldValue,
                         String newValue) {
        registerGlobal(action, entityName, entityId, description, oldValue, newValue);
    }

    /*
     * Registro global explícito.
     * Usar para acciones sin planta: crear usuario global, actualizar usuario global,
     * cambios técnicos de plataforma, etc.
     */
    public void registerGlobal(String action,
                               String entityName,
                               Long entityId,
                               String description,
                               String oldValue,
                               String newValue) {
        String username = getCurrentUsername();

        AuditLog auditLog = new AuditLog(
                null,
                username,
                action,
                entityName,
                entityId,
                description,
                oldValue,
                newValue
        );

        auditLogRepository.save(auditLog);
    }

    /*
     * Registro asociado a planta.
     * Usar para acciones operativas: roles por planta, sucesión, disponibilidad,
     * publicación de display, miembros de planta, etc.
     */
    public void registerForPlant(Plant plant,
                                 String action,
                                 String entityName,
                                 Long entityId,
                                 String description,
                                 String oldValue,
                                 String newValue) {
        String username = getCurrentUsername();

        AuditLog auditLog = new AuditLog(
                plant,
                username,
                action,
                entityName,
                entityId,
                description,
                oldValue,
                newValue
        );

        auditLogRepository.save(auditLog);
    }

    public List<AuditLog> findLatest() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public List<AuditLog> findLatestByPlantCode(String plantCode) {
        return auditLogRepository.findTop100ByPlant_CodeOrderByCreatedAtDesc(
                normalizePlantCode(plantCode)
        );
    }

    public List<AuditLog> findLatestGlobal() {
        return auditLogRepository.findTop100ByPlantIsNullOrderByCreatedAtDesc();
    }

    public Page<AuditLog> searchAuditLogs(String plantCode,
                                         boolean onlyGlobal,
                                         String action,
                                         String username,
                                         LocalDate from,
                                         LocalDate to,
                                         int page,
                                         int size) {
        if (onlyGlobal && plantCode != null && !plantCode.trim().isBlank()) {
            throw new IllegalArgumentException("No se puede combinar plantCode con onlyGlobal.");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
        );

        PageRequest pageable = PageRequest.of(safePage, safeSize, sort);

        return auditLogRepository.findAll(
                buildSpecification(plantCode, onlyGlobal, action, username, from, to),
                pageable
        );
    }

    private Specification<AuditLog> buildSpecification(
            String plantCode,
            boolean onlyGlobal,
            String action,
            String username,
            LocalDate from,
            LocalDate to
    ) {
        List<Specification<AuditLog>> filters = new ArrayList<>();

        if (onlyGlobal) {
            filters.add(AuditLogSpecifications.onlyGlobal());
        } else if (plantCode != null) {
            filters.add(AuditLogSpecifications.plantCode(plantCode));
        }

        filters.add(AuditLogSpecifications.actionEquals(action));
        filters.add(AuditLogSpecifications.usernameContains(username));
        filters.add(AuditLogSpecifications.createdAtGreaterThanOrEqual(from));
        filters.add(AuditLogSpecifications.createdAtBefore(to));

        Specification<AuditLog> combined = null;

        for (Specification<AuditLog> filter : filters) {
            if (filter == null) {
                continue;
            }

            combined = combined == null ? Specification.where(filter) : combined.and(filter);
        }

        return combined;
    }

    public String getCurrentUsername() {
        return currentUserResolver.resolveCurrentUser()
                .map(AppUser::getUsername)
                .orElseGet(this::getCurrentUsernameFallback);
    }

    private String getCurrentUsernameFallback() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            return "usuario_desarrollo";
        }

        String username = authentication.getName();

        if ("anonymousUser".equals(username)) {
            return "usuario_desarrollo";
        }

        return username;
    }

    private String normalizePlantCode(String plantCode) {
        return plantCode == null ? null : plantCode.trim().toLowerCase();
    }
}