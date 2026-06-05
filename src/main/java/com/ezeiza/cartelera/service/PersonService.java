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
    private final AuditService auditService;


    public PersonService(PersonRepository personRepository,
                         SuccessionOrderRepository successionOrderRepository,
                         AuditService auditService) {
        this.personRepository = personRepository;
        this.successionOrderRepository = successionOrderRepository;
        this.auditService = auditService;
    }

    public List<Person> findActivePersons() {
        return personRepository.findByActiveTrueOrderByLastNameAscFirstNameAsc();
    }

    @Transactional
    public Person createPerson(String firstName, String lastName, String position) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }

        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("El apellido es obligatorio");
        }

        String cleanFirstName = firstName.trim();
        String cleanLastName = lastName.trim();
        String cleanPosition = position != null ? position.trim() : "";

        boolean alreadyExists = personRepository
                .existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndActiveTrue(
                        cleanFirstName,
                        cleanLastName
                );

        if (alreadyExists) {
            throw new IllegalArgumentException(
                    "Ya existe una persona activa con ese nombre y apellido"
            );
        }

        Person person = new Person(cleanFirstName, cleanLastName, cleanPosition);
        Person savedPerson = personRepository.save(person);

        Integer maxOrder = successionOrderRepository.findMaxActiveOrderNumber();

        SuccessionOrder successionOrder = new SuccessionOrder(savedPerson, maxOrder + 1);
        successionOrderRepository.save(successionOrder);

        auditService.register(
                "CREATE_PERSON",
                "Person",
                savedPerson.getId(),
                "Se creó la persona " + savedPerson.getFullName(),
                null,
                "available=false, active=true, orderNumber=" + successionOrder.getOrderNumber()
        );

        return savedPerson;
    }

    @Transactional
    public void updateAvailability(Long personId, boolean available) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));

        boolean oldAvailability = person.isAvailable();

        person.setAvailable(available);
        personRepository.save(person);

        String newAvailabilityText = available ? "Disponible" : "No disponible";

        auditService.register(
                "CHANGE_AVAILABILITY",
                "Person",
                person.getId(),
                "Se cambió la disponibilidad de " + person.getFullName() + " a " + newAvailabilityText,
                "available=" + oldAvailability,
                "available=" + available
        );
    }

    @Transactional
    public void moveUp(Long personId) {
        SuccessionOrder current = successionOrderRepository.findByPersonIdAndActiveTrue(personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no está en la lista de sucesión"));

        if (current.getOrderNumber() <= 1) {
            return;
        }

        Integer totalPositions = successionOrderRepository.findMaxActiveOrderNumber();

        SuccessionOrder previous = successionOrderRepository
                .findByOrderNumberAndActiveTrue(current.getOrderNumber() - 1)
                .orElse(null);

        if (previous == null) {
            normalizeSuccessionOrder();
            return;
        }

        Integer oldOrder = current.getOrderNumber();
        Integer previousOrder = previous.getOrderNumber();

        current.setOrderNumber(previousOrder);
        previous.setOrderNumber(oldOrder);

        successionOrderRepository.save(previous);
        successionOrderRepository.save(current);

        auditService.register(
                "MOVE_UP",
                "SuccessionOrder",
                current.getId(),
                "Se subió en la sucesión a " + current.getPerson().getFullName()
                        + " a la posición " + current.getOrderNumber() + "/" + totalPositions,
                "orderNumber=" + oldOrder,
                "orderNumber=" + current.getOrderNumber()
        );
    }

    @Transactional
    public void moveDown(Long personId) {
        SuccessionOrder current = successionOrderRepository.findByPersonIdAndActiveTrue(personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no está en la lista de sucesión"));

        Integer totalPositions = successionOrderRepository.findMaxActiveOrderNumber();

        if (current.getOrderNumber() >= totalPositions) {
            return;
        }

        SuccessionOrder next = successionOrderRepository
                .findByOrderNumberAndActiveTrue(current.getOrderNumber() + 1)
                .orElse(null);

        if (next == null) {
            normalizeSuccessionOrder();
            return;
        }

        Integer oldOrder = current.getOrderNumber();
        Integer nextOrder = next.getOrderNumber();

        current.setOrderNumber(nextOrder);
        next.setOrderNumber(oldOrder);

        successionOrderRepository.save(next);
        successionOrderRepository.save(current);

        auditService.register(
                "MOVE_DOWN",
                "SuccessionOrder",
                current.getId(),
                "Se bajó en la sucesión a " + current.getPerson().getFullName()
                        + " a la posición " + current.getOrderNumber() + "/" + totalPositions,
                "orderNumber=" + oldOrder,
                "orderNumber=" + current.getOrderNumber()
        );
    }

    @Transactional
    public void removeFromSuccession(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));

        SuccessionOrder successionOrder = successionOrderRepository.findByPersonIdAndActiveTrue(personId)
                .orElseThrow(() -> new IllegalArgumentException("La persona no está en la lista de sucesión"));

        boolean oldAvailable = person.isAvailable();
        person.setActive(false);
        person.setAvailable(false);
        successionOrder.setActive(false);

        personRepository.save(person);
        successionOrderRepository.save(successionOrder);

        normalizeSuccessionOrder();

        auditService.register(
                "REMOVE_PERSON",
                "Person",
                person.getId(),
                "Se eliminó de la lista a " + person.getFullName(),
                "active=true, available=" + oldAvailable,
                "active=false, available=false"
        );
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