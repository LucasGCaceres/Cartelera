package com.ezeiza.cartelera.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "display_published")
public class DisplayPublished {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long personId;

    private String responsibleName;

    private String responsiblePosition;

    private String plantName;

    private String mainTitle;

    private LocalDateTime publishedAt;

    public DisplayPublished() {
    }

    public DisplayPublished(Long personId,
                            String responsibleName,
                            String responsiblePosition,
                            String plantName,
                            String mainTitle) {
        this.personId = personId;
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

    public Long getPersonId() {
        return personId;
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

    public void setPersonId(Long personId) {
        this.personId = personId;
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