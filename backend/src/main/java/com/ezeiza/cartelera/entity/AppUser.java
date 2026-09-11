package com.ezeiza.cartelera.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "app_users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_app_users_corporate_email", columnNames = "corporate_email")
        }
)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "corporate_email", nullable = false, unique = true)
    private String corporateEmail;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String passwordHash;

    private boolean active = true;

    private boolean platformAdmin = false;

    private String entraObjectId;

    private String entraTenantId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public AppUser() {
    }

    public AppUser(String username,
                   String corporateEmail,
                   String passwordHash,
                   String fullName,
                   boolean platformAdmin) {
        this.username = normalizeUsername(username);
        this.corporateEmail = normalizeEmail(corporateEmail);
        this.passwordHash = passwordHash;
        this.fullName = clean(fullName);
        this.platformAdmin = platformAdmin;
        this.active = true;
    }

    @PrePersist
    public void prePersist() {
        normalizeFields();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        normalizeFields();
        updatedAt = LocalDateTime.now();
    }

    private void normalizeFields() {
        username = normalizeUsername(username);
        corporateEmail = normalizeEmail(corporateEmail);
        fullName = clean(fullName);
    }

    private static String normalizeUsername(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private static String normalizeEmail(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private static String clean(String value) {
        return value == null ? null : value.trim();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getCorporateEmail() {
        return corporateEmail;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public String getEntraObjectId() {
        return entraObjectId;
    }

    public String getEntraTenantId() {
        return entraTenantId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setCorporateEmail(String corporateEmail) {
        this.corporateEmail = corporateEmail;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setPlatformAdmin(boolean platformAdmin) {
        this.platformAdmin = platformAdmin;
    }

    public void setEntraObjectId(String entraObjectId) {
        this.entraObjectId = entraObjectId;
    }

    public void setEntraTenantId(String entraTenantId) {
        this.entraTenantId = entraTenantId;
    }
}