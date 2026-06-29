package com.ezeiza.cartelera.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "display_published",
        indexes = {
                @Index(name = "idx_display_published_plant_published_at", columnList = "plant_id, publishedAt")
        }
)
public class DisplayPublished {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    private Long userId;

    private String responsibleName;

    private String responsiblePosition;

    private String plantName;

    private String mainTitle;

    private LocalDateTime publishedAt;

    public DisplayPublished() {
    }

    public DisplayPublished(Plant plant,
                            Long userId,
                            String responsibleName,
                            String responsiblePosition,
                            String plantName,
                            String mainTitle) {
        this.plant = plant;
        this.userId = userId;
        this.responsibleName = responsibleName;
        this.responsiblePosition = responsiblePosition;
        this.plantName = plantName;
        this.mainTitle = mainTitle;
        this.publishedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Plant getPlant() {
        return plant;
    }

    public Long getUserId() {
        return userId;
    }

    public String getResponsibleName() {
        return responsibleName;
    }

    public String getResponsiblePosition() {
        return responsiblePosition;
    }

    public String getPlantName() {
        return plantName;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setResponsibleName(String responsibleName) {
        this.responsibleName = responsibleName;
    }

    public void setResponsiblePosition(String responsiblePosition) {
        this.responsiblePosition = responsiblePosition;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
