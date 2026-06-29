package com.ezeiza.cartelera.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "plant_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_plant_members_plant_user",
                        columnNames = {"plant_id", "user_id"}
                )
        }
)
public class PlantMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    private String position;

    private boolean available = false;

    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public PlantMember() {
    }

    public PlantMember(Plant plant, AppUser user, String position) {
        this.plant = plant;
        this.user = user;
        this.position = clean(position);
        this.available = false;
        this.active = true;
    }

    @PrePersist
    public void prePersist() {
        position = clean(position);
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        position = clean(position);
        updatedAt = LocalDateTime.now();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public Long getId() {
        return id;
    }

    public Plant getPlant() {
        return plant;
    }

    public AppUser getUser() {
        return user;
    }

    public String getPosition() {
        return position;
    }

    public boolean isAvailable() {
        return available;
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

    public String getDisplayName() {
        return user != null ? user.getFullName() : "";
    }

    public String getEmail() {
        return user != null ? user.getCorporateEmail() : "";
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}