package com.social.assistance.repository;

import com.social.assistance.model.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ParameterRepository extends JpaRepository<Parameter, Integer> {

    Page<Parameter> findByCategory(String category, Pageable pageable);

    boolean existsByCategoryAndValue(String category, String value);
}
