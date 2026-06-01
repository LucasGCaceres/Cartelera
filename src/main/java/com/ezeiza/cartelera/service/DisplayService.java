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

    public DisplayService(SuccessionOrderRepository successionOrderRepository,
                          DisplayPublishedRepository displayPublishedRepository) {
        this.successionOrderRepository = successionOrderRepository;
        this.displayPublishedRepository = displayPublishedRepository;
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

        return displayPublishedRepository.save(published);
    }

    public Optional<DisplayPublished> getLastPublishedDisplay() {
        return displayPublishedRepository.findTopByOrderByIdDesc();
    }
}