package com.ezeiza.cartelera.dto.audit;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedAuditLogResponse(
        List<AuditLogEntryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static PagedAuditLogResponse from(Page<AuditLogEntryResponse> page) {
        return new PagedAuditLogResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
