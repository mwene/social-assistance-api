package com.social.assistance.repository;

import com.social.assistance.model.County;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountyRepository extends JpaRepository<County, Integer> {
    Optional<County> findByName(String name);
}
