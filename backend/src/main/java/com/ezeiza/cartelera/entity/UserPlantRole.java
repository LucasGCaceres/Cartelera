package com.ezeiza.cartelera.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_plant_roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_plant_role_user_plant",
                        columnNames = {"user_id", "plant_id"}
                )
        }
)
public class UserPlantRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlantRole role;

    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public UserPlantRole() {
    }

    public UserPlantRole(AppUser user, Plant plant, PlantRole role) {
        this.user = user;
        this.plant = plant;
        this.role = role;
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

    public AppUser getUser() {
        return user;
    }

    public Plant getPlant() {
        return plant;
    }

    public PlantRole getRole() {
        return role;
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

    public void setUser(AppUser user) {
        this.user = user;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public void setRole(PlantRole role) {
        this.role = role;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}