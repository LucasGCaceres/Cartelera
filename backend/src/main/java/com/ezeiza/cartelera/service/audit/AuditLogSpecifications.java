package com.ezeiza.cartelera.service.audit;

import com.ezeiza.cartelera.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> plantCode(String plantCode) {
        if (plantCode == null || plantCode.trim().isBlank()) {
            return null;
        }

        String normalizedCode = plantCode.trim().toLowerCase();

        return (root, query, criteriaBuilder) -> {
            Path<String> plantCodePath = root.join("plant").get("code");
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(plantCodePath),
                    normalizedCode
            );
        };
    }

    public static Specification<AuditLog> onlyGlobal() {
        return (root, query, criteriaBuilder) -> root.get("plant").isNull();
    }

    public static Specification<AuditLog> actionEquals(String action) {
        if (action == null || action.trim().isBlank()) {
            return null;
        }

        String normalizedAction = action.trim().toUpperCase();

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                criteriaBuilder.upper(root.get("action")),
                normalizedAction
        );
    }

    public static Specification<AuditLog> usernameContains(String username) {
        if (username == null || username.trim().isBlank()) {
            return null;
        }

        String normalizedUsername = "%" + username.trim().toLowerCase() + "%";

        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get("username")),
                normalizedUsername
        );
    }

    public static Specification<AuditLog> createdAtGreaterThanOrEqual(LocalDate from) {
        if (from == null) {
            return null;
        }

        LocalDateTime fromStart = from.atStartOfDay();

        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(
                root.get("createdAt"),
                fromStart
        );
    }

    public static Specification<AuditLog> createdAtBefore(LocalDate to) {
        if (to == null) {
            return null;
        }

        if (to.equals(LocalDate.MAX)) {
            return null;
        }

        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThan(
                root.get("createdAt"),
                toExclusive
        );
    }
}
