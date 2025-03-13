package com.social.assistance.repository;

import com.social.assistance.model.SubCounty;
import com.social.assistance.model.County;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubCountyRepository extends JpaRepository<SubCounty, Integer> {
    Optional<SubCounty> findByNameAndCounty(String name, County county);
}
