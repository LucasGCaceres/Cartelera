package com.ezeiza.cartelera.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "succession_orders",
        indexes = {
                @Index(name = "idx_succession_orders_plant_order", columnList = "plant_id, orderNumber")
        }
)
public class SuccessionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_member_id", nullable = false)
    private PlantMember plantMember;

    @Column(nullable = false)
    private Integer orderNumber;

    private boolean active = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public SuccessionOrder() {
    }

    public SuccessionOrder(Plant plant, PlantMember plantMember, Integer orderNumber) {
        this.plant = plant;
        this.plantMember = plantMember;
        this.orderNumber = orderNumber;
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

    public Plant getPlant() {
        return plant;
    }

    public PlantMember getPlantMember() {
        return plantMember;
    }

    public Integer getOrderNumber() {
        return orderNumber;
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

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public void setPlantMember(PlantMember plantMember) {
        this.plantMember = plantMember;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}