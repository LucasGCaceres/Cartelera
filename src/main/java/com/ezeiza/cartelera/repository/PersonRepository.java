package com.ezeiza.cartelera.repository;

import com.ezeiza.cartelera.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findByActiveTrueOrderByLastNameAscFirstNameAsc();

    boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndActiveTrue(
            String firstName,
            String lastName
    );
}