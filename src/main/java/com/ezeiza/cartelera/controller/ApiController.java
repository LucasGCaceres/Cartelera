package com.ezeiza.cartelera.controller;

import com.ezeiza.cartelera.dto.AdminStateResponse;
import com.ezeiza.cartelera.dto.AvailabilityRequest;
import com.ezeiza.cartelera.dto.CreatePersonRequest;
import com.ezeiza.cartelera.dto.PersonResponse;
import com.ezeiza.cartelera.dto.SuccessionItemResponse;
import com.ezeiza.cartelera.entity.Person;
import com.ezeiza.cartelera.entity.SuccessionOrder;
import com.ezeiza.cartelera.repository.SuccessionOrderRepository;
import com.ezeiza.cartelera.service.DisplayService;
import com.ezeiza.cartelera.service.PersonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final PersonService personService;
    private final DisplayService displayService;
    private final SuccessionOrderRepository successionOrderRepository;

    public ApiController(PersonService personService,
                         DisplayService displayService,
                         SuccessionOrderRepository successionOrderRepository) {
        this.personService = personService;
        this.displayService = displayService;
        this.successionOrderRepository = successionOrderRepository;
    }

    @GetMapping("/admin/state")
    public AdminStateResponse getAdminState() {
        return buildAdminStateResponse();
    }

    @PostMapping("/persons")
    public AdminStateResponse createPerson(@RequestBody CreatePersonRequest request) {
        personService.createPerson(
                request.firstName(),
                request.lastName(),
                request.position()
        );

        return buildAdminStateResponse();
    }

    @PatchMapping("/persons/{id}/availability")
    public AdminStateResponse updateAvailability(@PathVariable Long id,
                                                 @RequestBody AvailabilityRequest request) {
        boolean available = Boolean.TRUE.equals(request.available());

        personService.updateAvailability(id, available);

        return buildAdminStateResponse();
    }

    @PostMapping("/persons/{id}/move-up")
    public AdminStateResponse moveUp(@PathVariable Long id) {
        personService.moveUp(id);

        return buildAdminStateResponse();
    }

    @PostMapping("/persons/{id}/move-down")
    public AdminStateResponse moveDown(@PathVariable Long id) {
        personService.moveDown(id);

        return buildAdminStateResponse();
    }

    @DeleteMapping("/persons/{id}")
    public AdminStateResponse removePerson(@PathVariable Long id) {
        personService.removeFromSuccession(id);

        return buildAdminStateResponse();
    }

    @GetMapping("/display/current")
    public PersonResponse getCurrentDisplayResponsible() {
        return displayService.calculateCurrentResponsible()
                .map(this::toPersonResponse)
                .orElse(null);
    }

    private AdminStateResponse buildAdminStateResponse() {
        PersonResponse currentResponsible = displayService.calculateCurrentResponsible()
                .map(this::toPersonResponse)
                .orElse(null);

        List<SuccessionItemResponse> successionList = successionOrderRepository
                .findByActiveTrueOrderByOrderNumberAsc()
                .stream()
                .map(this::toSuccessionItemResponse)
                .toList();

        return new AdminStateResponse(currentResponsible, successionList);
    }

    private SuccessionItemResponse toSuccessionItemResponse(SuccessionOrder item) {
        return new SuccessionItemResponse(
                item.getId(),
                item.getOrderNumber(),
                toPersonResponse(item.getPerson())
        );
    }

    private PersonResponse toPersonResponse(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getFullName(),
                person.getPosition(),
                person.isAvailable(),
                person.isActive()
        );
    }
}