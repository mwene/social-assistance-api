package com.social.assistance.repository;

import com.social.assistance.model.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    Page<Application> findByApplicantId(Integer applicantId, Pageable pageable);

    Page<Application> findByStatus(String status, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.applicant.id = :applicantId AND a.status = :status")
    Page<Application> findByApplicantIdAndStatus(Integer applicantId, String status, Pageable pageable);

    Page<Application> findByProgrammeId(Integer programmeId, Pageable pageable);

    @Query("SELECT COUNT(a) as total, " +
           "SUM(CASE WHEN a.status = 'Approved' THEN 1 ELSE 0 END) as approved, " +
           "SUM(CASE WHEN a.status = 'Pending' THEN 1 ELSE 0 END) as pending, " +
           "SUM(CASE WHEN a.status = 'Rejected' THEN 1 ELSE 0 END) as rejected, " +
           "p.name as programmeName " +
           "FROM Application a JOIN a.programme p " +
           "GROUP BY p.id, p.name")
    List<Object[]> getApplicationStatsByProgramme();

    @Query("SELECT a FROM Application a " +
           "JOIN a.applicant ap " +
           "JOIN ap.village v " +
           "JOIN v.subLocation sl " +
           "JOIN sl.location l " +
           "JOIN l.subCounty sc " +
           "JOIN sc.county c " +
           "WHERE (:status IS NULL OR a.status = :status) " +
           "AND a.applicationDate BETWEEN :startDate AND :endDate " +
           "AND (:age IS NULL OR ap.age = :age) " +
           "AND (:sexId IS NULL OR ap.sex.id = :sexId) " +
           "AND (:maritalStatusId IS NULL OR ap.maritalStatus.id = :maritalStatusId) " +
           "AND (:physicalLocationLevel = 'All' " +
           "     OR (:physicalLocationLevel = 'village' AND v.id = :physicalLocationId) " +
           "     OR (:physicalLocationLevel = 'subLocation' AND sl.id = :physicalLocationId) " +
           "     OR (:physicalLocationLevel = 'Location' AND l.id = :physicalLocationId) " +
           "     OR (:physicalLocationLevel = 'SubCounty' AND sc.id = :physicalLocationId) " +
           "     OR (:physicalLocationLevel = 'County' AND c.id = :physicalLocationId))")
    List<Application> findFilteredApplications(
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("age") Integer age,
            @Param("sexId") Integer sexId,
            @Param("maritalStatusId") Integer maritalStatusId,
            @Param("physicalLocationLevel") String physicalLocationLevel,
            @Param("physicalLocationId") Integer physicalLocationId);
}
