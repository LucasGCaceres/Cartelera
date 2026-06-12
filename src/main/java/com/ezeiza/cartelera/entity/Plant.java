package com.ezeiza.cartelera.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plants")
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String displayTitle = "Responsable de planta";

    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Plant() {
    }

    public Plant(String code, String name, String displayTitle) {
        this.code = code;
        this.name = name;
        this.displayTitle = displayTitle;
        this.active = true;
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getDisplayTitle() {
        return displayTitle;
    }

    public boolean isActive() {
        return active;
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

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}