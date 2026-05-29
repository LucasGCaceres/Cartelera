package com.ezeiza.cartelera.service;

import com.ezeiza.cartelera.entity.Person;
import com.ezeiza.cartelera.entity.SuccessionOrder;
import com.ezeiza.cartelera.repository.PersonRepository;
import com.ezeiza.cartelera.repository.SuccessionOrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final SuccessionOrderRepository successionOrderRepository;

    public PersonService(PersonRepository personRepository,
                         SuccessionOrderRepository successionOrderRepository) {
        this.personRepository = personRepository;
        this.successionOrderRepository = successionOrderRepository;
    }

    public List<Person> findActivePersons() {
        return personRepository.findByActiveTrueOrderByLastNameAscFirstNameAsc();
    }

    @Transactional
    public Person createPerson(String firstName, String lastName, String position) {
        Person person = new Person(firstName, lastName, position);
        Person savedPerson = personRepository.save(person);

        Integer maxOrder = successionOrderRepository.findMaxActiveOrderNumber();

        SuccessionOrder successionOrder = new SuccessionOrder(savedPerson, maxOrder + 1);
        successionOrderRepository.save(successionOrder);

        return savedPerson;
    }

    @Transactional
    public void updateAvailability(Long personId, boolean available) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));

        person.setAvailable(available);
        personRepository.save(person);
    }

    @Transactional
    public void moveUp(Long personId) {
        SuccessionOrder current = successionOrderRepository.findByPersonIdAndActiveTrue(personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no está en la lista de sucesión"));

        if (current.getOrderNumber() <= 1) {
            return;
        }

        SuccessionOrder previous = successionOrderRepository
                .findByOrderNumberAndActiveTrue(current.getOrderNumber() - 1)
                .orElse(null);

        if (previous == null) {
            normalizeSuccessionOrder();
            return;
        }

        Integer currentOrder = current.getOrderNumber();

        current.setOrderNumber(previous.getOrderNumber());
        previous.setOrderNumber(currentOrder);

        successionOrderRepository.save(previous);
        successionOrderRepository.save(current);
    }

    @Transactional
    public void moveDown(Long personId) {
        SuccessionOrder current = successionOrderRepository.findByPersonIdAndActiveTrue(personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no está en la lista de sucesión"));

        Integer maxOrder = successionOrderRepository.findMaxActiveOrderNumber();

        if (current.getOrderNumber() >= maxOrder) {
            return;
        }

        SuccessionOrder next = successionOrderRepository
                .findByOrderNumberAndActiveTrue(current.getOrderNumber() + 1)
                .orElse(null);

        if (next == null) {
            normalizeSuccessionOrder();
            return;
        }

        Integer currentOrder = current.getOrderNumber();

        current.setOrderNumber(next.getOrderNumber());
        next.setOrderNumber(currentOrder);

        successionOrderRepository.save(next);
        successionOrderRepository.save(current);
    }

    @Transactional
    public void removeFromSuccession(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));

        SuccessionOrder successionOrder = successionOrderRepository.findByPersonIdAndActiveTrue(personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no está en la lista de sucesión"));

        person.setActive(false);
        person.setAvailable(false);
        successionOrder.setActive(false);

        personRepository.save(person);
        successionOrderRepository.save(successionOrder);

        normalizeSuccessionOrder();
    }

    @Transactional
    public void normalizeSuccessionOrder() {
        List<SuccessionOrder> activeList = successionOrderRepository.findByActiveTrueOrderByOrderNumberAsc();

        int order = 1;

        for (SuccessionOrder item : activeList) {
            item.setOrderNumber(order);
            successionOrderRepository.save(item);
            order++;
        }
    }
}