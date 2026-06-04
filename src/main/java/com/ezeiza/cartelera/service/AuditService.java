package com.ezeiza.cartelera.service;

import com.ezeiza.cartelera.entity.AuditLog;
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

    public void register(String action,
                         String entityName,
                         Long entityId,
                         String description,
                         String oldValue,
                         String newValue) {
        String username = getCurrentUsername();

        AuditLog auditLog = new AuditLog(
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

    private String getCurrentUsername() {
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
}