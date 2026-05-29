package com.ezeiza.cartelera.service;

import com.ezeiza.cartelera.entity.Person;
import com.ezeiza.cartelera.entity.SuccessionOrder;
import com.ezeiza.cartelera.repository.SuccessionOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DisplayService {

    private final SuccessionOrderRepository successionOrderRepository;

    public DisplayService(SuccessionOrderRepository successionOrderRepository) {
        this.successionOrderRepository = successionOrderRepository;
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
}