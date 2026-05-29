package com.ezeiza.cartelera.controller;

import com.ezeiza.cartelera.entity.Person;
import com.ezeiza.cartelera.entity.SuccessionOrder;
import com.ezeiza.cartelera.repository.SuccessionOrderRepository;
import com.ezeiza.cartelera.service.DisplayService;
import com.ezeiza.cartelera.service.PersonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class HomeController {

    private final PersonService personService;
    private final DisplayService displayService;
    private final SuccessionOrderRepository successionOrderRepository;

    public HomeController(PersonService personService,
                          DisplayService displayService,
                          SuccessionOrderRepository successionOrderRepository) {
        this.personService = personService;
        this.displayService = displayService;
        this.successionOrderRepository = successionOrderRepository;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/admin";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        List<Person> persons = personService.findActivePersons();
        List<SuccessionOrder> successionList =
                successionOrderRepository.findByActiveTrueOrderByOrderNumberAsc();

        model.addAttribute("persons", persons);
        model.addAttribute("successionList", successionList);
        model.addAttribute("currentResponsible", displayService.calculateCurrentResponsible().orElse(null));

        return "admin";
    }

    @PostMapping("/persons")
    public String createPerson(@RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String position) {
        personService.createPerson(firstName, lastName, position);
        return "redirect:/admin";
    }

    @PostMapping("/persons/{id}/availability")
    public String updateAvailability(@PathVariable Long id,
                                     @RequestParam(required = false) String available) {
        personService.updateAvailability(id, available != null);
        return "redirect:/admin";
    }

    @PostMapping("/persons/{id}/move-up")
    public String moveUp(@PathVariable Long id) {
        personService.moveUp(id);
        return "redirect:/admin";
    }

    @PostMapping("/persons/{id}/move-down")
    public String moveDown(@PathVariable Long id) {
        personService.moveDown(id);
        return "redirect:/admin";
    }

    @PostMapping("/persons/{id}/remove")
    public String removeFromSuccession(@PathVariable Long id) {
        personService.removeFromSuccession(id);
        return "redirect:/admin";
    }

    @GetMapping("/display")
    public String display(Model model) {
        model.addAttribute("plantName", "PLANTA EZEIZA");
        model.addAttribute("mainTitle", "Responsable de planta");
        model.addAttribute("responsible", displayService.calculateCurrentResponsible().orElse(null));

        return "display";
    }
}