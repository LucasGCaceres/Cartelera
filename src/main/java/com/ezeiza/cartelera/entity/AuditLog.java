package com.ezeiza.cartelera.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String action;

    private String entityName;

    private Long entityId;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String oldValue;

    @Column(length = 1000)
    private String newValue;

    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(String username,
                    String action,
                    String entityName,
                    Long entityId,
                    String description,
                    String oldValue,
                    String newValue) {
        this.username = username;
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.description = description;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getEntityName() {
        return entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDescription() {
        return description;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}