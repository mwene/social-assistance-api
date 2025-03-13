package com.social.assistance.repository;

import com.social.assistance.model.Location;
import com.social.assistance.model.SubCounty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {
    Optional<Location> findByNameAndSubCounty(String name, SubCounty subCounty);
}
