package com.ezeiza.cartelera.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "plants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_plants_code", columnNames = "code")
        }
)
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Slug usado en URLs: ezeiza, aeroparque, the-pro-laundry.
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String displayName;

    // Se mantiene como título principal de cartelera.
    @Column(nullable = false)
    private String displayTitle = "Responsable de planta";

    private boolean active = true;

    private Integer sortOrder = 0;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Plant() {
    }

    public Plant(String code, String name, String displayTitle) {
        this.code = normalizeCode(code);
        this.name = clean(name);
        this.displayName = clean(name);
        this.displayTitle = clean(displayTitle);
        this.active = true;
    }

    public Plant(String code,
                 String name,
                 String displayName,
                 String displayTitle,
                 Integer sortOrder) {
        this.code = normalizeCode(code);
        this.name = clean(name);
        this.displayName = clean(displayName);
        this.displayTitle = clean(displayTitle);
        this.sortOrder = sortOrder != null ? sortOrder : 0;
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
        code = normalizeCode(code);
        name = clean(name);
        displayName = clean(displayName);
        displayTitle = clean(displayTitle);

        if (displayName == null || displayName.isBlank()) {
            displayName = name;
        }

        if (displayTitle == null || displayTitle.isBlank()) {
            displayTitle = "Responsable de planta";
        }

        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    private static String normalizeCode(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim().toLowerCase();

        if (trimmed.isEmpty()) {
            return trimmed;
        }

        return trimmed.replaceAll("\\s+", "-");
    }
    private static String clean(String value) {
        return value == null ? null : value.trim();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public boolean isActive() {
        return active;
    }

    public Integer getSortOrder() {
        return sortOrder;
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

    public void setCode(String code) {
        this.code = code;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}