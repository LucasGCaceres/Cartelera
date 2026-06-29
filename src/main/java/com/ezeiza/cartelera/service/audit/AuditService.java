package com.ezeiza.cartelera.service.audit;

import com.ezeiza.cartelera.entity.AuditLog;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.repository.AuditLogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
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

    public String getCurrentUsername() {
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