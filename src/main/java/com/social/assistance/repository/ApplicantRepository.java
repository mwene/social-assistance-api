package com.social.assistance.repository;

import com.social.assistance.model.Applicant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Integer> {

    boolean existsByIdNumber(String idNumber);

    @Query("SELECT a FROM Applicant a WHERE LOWER(a.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Applicant> findByNameContaining(String name, Pageable pageable);

    Page<Applicant> findByVerificationStatus(String verificationStatus, Pageable pageable);

    Page<Applicant> findByVillageId(Integer villageId, Pageable pageable);

    Optional<Applicant> findByFirstNameAndLastNameAndMiddleName(String firstName, String lastName, String middleName);
    
    @Query("SELECT a FROM Applicant a LEFT JOIN Application app ON a.id = app.applicant.id " +
       "WHERE (:name IS NULL OR CONCAT(a.firstName, ' ', a.middleName, ' ', a.lastName) LIKE %:name%) " +
       "AND (:idNumber IS NULL OR a.idNumber = :idNumber) " +
       "AND (:dateApplied IS NULL OR app.applicationDate = :dateApplied)")
Page<Applicant> findByFilters(
    @Param("name") String name,
    @Param("idNumber") String idNumber,
    @Param("dateApplied") LocalDate dateApplied,
    Pageable pageable);
}
