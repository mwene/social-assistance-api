package com.social.assistance.repository;

import com.social.assistance.model.SubLocation;
import com.social.assistance.model.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//Ensure ParameterRepository and VillageRepository have the necessary methods (findByCategory, findAll with Pageable)
@Repository
public interface VillageRepository extends JpaRepository<Village, Integer> {

    List<Village> findBySubLocationId(Integer subLocationId);

    boolean existsByNameAndSubLocationId(String name, Integer subLocationId);
    
    Optional<Village> findByNameAndSubLocation(String name, SubLocation subLocation);
}
