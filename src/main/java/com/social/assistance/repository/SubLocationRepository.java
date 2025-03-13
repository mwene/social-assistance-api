package com.social.assistance.repository;

import com.social.assistance.model.SubLocation;
import com.social.assistance.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubLocationRepository extends JpaRepository<SubLocation, Integer> {
    Optional<SubLocation> findByNameAndLocation(String name, Location location);
}
