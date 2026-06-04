package com.ezeiza.cartelera.service;

import com.ezeiza.cartelera.entity.DisplayPublished;
import com.ezeiza.cartelera.entity.Person;
import com.ezeiza.cartelera.entity.SuccessionOrder;
import com.ezeiza.cartelera.repository.DisplayPublishedRepository;
import com.ezeiza.cartelera.repository.SuccessionOrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DisplayService {

    private static final String DEFAULT_PLANT_NAME = "PLANTA EZEIZA";
    private static final String DEFAULT_MAIN_TITLE = "Responsable de planta";

    private final SuccessionOrderRepository successionOrderRepository;
    private final DisplayPublishedRepository displayPublishedRepository;
    private final AuditService auditService;

    public DisplayService(SuccessionOrderRepository successionOrderRepository,
                          DisplayPublishedRepository displayPublishedRepository,
                          AuditService auditService) {
        this.successionOrderRepository = successionOrderRepository;
        this.displayPublishedRepository = displayPublishedRepository;
        this.auditService = auditService;
    }

    public Optional<Person> calculateCurrentResponsible() {
        List<SuccessionOrder> successionList =
                successionOrderRepository.findByActiveTrueOrderByOrderNumberAsc();

        return successionList.stream()
                .map(SuccessionOrder::getPerson)
                .filter(Person::isActive)
                .filter(Person::isAvailable)
                .findFirst();
    }

    @Transactional
    public DisplayPublished publishCurrentDisplay() {
        Optional<Person> responsibleOptional = calculateCurrentResponsible();

        DisplayPublished published;

        if (responsibleOptional.isPresent()) {
            Person responsible = responsibleOptional.get();

            published = new DisplayPublished(
                    responsible.getId(),
                    responsible.getFullName(),
                    responsible.getPosition(),
                    DEFAULT_PLANT_NAME,
                    DEFAULT_MAIN_TITLE
            );
        } else {
            published = new DisplayPublished(
                    null,
                    null,
                    null,
                    DEFAULT_PLANT_NAME,
                    DEFAULT_MAIN_TITLE
            );
        }

        DisplayPublished savedPublished = displayPublishedRepository.save(published);

        auditService.register(
                "PUBLISH_DISPLAY",
                "DisplayPublished",
                savedPublished.getId(),
                savedPublished.getResponsibleName() != null
                        ? "Se publicó la cartelera con responsable " + savedPublished.getResponsibleName()
                        : "Se publicó la cartelera sin responsable disponible",
                null,
                "responsibleName=" + savedPublished.getResponsibleName()
        );

        return savedPublished;    }

    public Optional<DisplayPublished> getLastPublishedDisplay() {
        return displayPublishedRepository.findTopByOrderByIdDesc();
    }
}