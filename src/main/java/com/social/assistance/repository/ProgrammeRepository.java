package com.social.assistance.repository;

import com.social.assistance.model.Programme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, Integer> {

    boolean existsByName(String name);

    Optional<Programme> findByName(String name);
}
