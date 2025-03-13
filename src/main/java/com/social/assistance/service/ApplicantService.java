package com.social.assistance.service;

import com.social.assistance.exception.DuplicateResourceException;
import com.social.assistance.exception.ResourceNotFoundException;
import com.social.assistance.model.Applicant;
import com.social.assistance.model.MakerCheckerLog;
import com.social.assistance.model.Parameter;
import com.social.assistance.model.User;
import com.social.assistance.model.Village;
import com.social.assistance.repository.ApplicantRepository;
import com.social.assistance.repository.MakerCheckerLogRepository;
import com.social.assistance.repository.ParameterRepository;
import com.social.assistance.repository.UserRepository;
import com.social.assistance.repository.VillageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class ApplicantService {

    private final ApplicantRepository applicantRepository;
    private final UserRepository userRepository;
    private final MakerCheckerLogRepository makerCheckerLogRepository;
    private final ParameterRepository parameterRepository;
    private final VillageRepository villageRepository;
    private final UserService userService; // Added for username-to-ID lookup

    @PersistenceContext
    private EntityManager entityManager; // For stored procedures (optional)

    public ApplicantService(
            ApplicantRepository applicantRepository,
            UserRepository userRepository,
            MakerCheckerLogRepository makerCheckerLogRepository,
            ParameterRepository parameterRepository,
            VillageRepository villageRepository,
            UserService userService) {
        this.applicantRepository = applicantRepository;
        this.userRepository = userRepository;
        this.makerCheckerLogRepository = makerCheckerLogRepository;
        this.parameterRepository = parameterRepository;
        this.villageRepository = villageRepository;
        this.userService = userService;
    }

    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    @Transactional
    public Applicant createApplicant(Applicant applicant) {
        if (applicantRepository.existsByIdNumber(applicant.getIdNumber())) {
            throw new DuplicateResourceException("Applicant with this ID number already exists");
        }

        // Fetch and set Parameter and Village entities
        Parameter sex = parameterRepository.findById(applicant.getSex().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sex parameter not found with ID: " + applicant.getSex().getId()));
        Parameter maritalStatus = parameterRepository.findById(applicant.getMaritalStatus().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Marital status parameter not found with ID: " + applicant.getMaritalStatus().getId()));
        Village village = villageRepository.findById(applicant.getVillage().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Village not found with ID: " + applicant.getVillage().getId()));

        applicant.setSex(sex);
        applicant.setMaritalStatus(maritalStatus);
        applicant.setVillage(village);
        applicant.setVerificationStatus("Pending");

        return applicantRepository.save(applicant);
    }

    public Optional<Applicant> getApplicantById(Integer id) {
        return applicantRepository.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_COLLECTOR')")
    public Page<Applicant> getAllApplicants(Pageable pageable) {
        return applicantRepository.findAll(pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_COLLECTOR')")
    @Transactional
    public Applicant updateApplicant(Integer id, Applicant updatedApplicant) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found"));

        if (!applicant.getIdNumber().equals(updatedApplicant.getIdNumber()) &&
                applicantRepository.existsByIdNumber(updatedApplicant.getIdNumber())) {
            throw new DuplicateResourceException("ID number already in use by another applicant");
        }

        // Fetch and set Parameter and Village entities
        Parameter sex = parameterRepository.findById(updatedApplicant.getSex().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sex parameter not found with ID: " + updatedApplicant.getSex().getId()));
        Parameter maritalStatus = parameterRepository.findById(updatedApplicant.getMaritalStatus().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Marital status parameter not found with ID: " + updatedApplicant.getMaritalStatus().getId()));
        Village village = villageRepository.findById(updatedApplicant.getVillage().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Village not found with ID: " + updatedApplicant.getVillage().getId()));

        applicant.setFirstName(updatedApplicant.getFirstName());
        applicant.setMiddleName(updatedApplicant.getMiddleName());
        applicant.setLastName(updatedApplicant.getLastName());
        applicant.setSex(sex);
        applicant.setAge(updatedApplicant.getAge());
        applicant.setMaritalStatus(maritalStatus);
        applicant.setIdNumber(updatedApplicant.getIdNumber());
        applicant.setVillage(village);
        applicant.setPostalAddress(updatedApplicant.getPostalAddress());
        applicant.setPhysicalAddress(updatedApplicant.getPhysicalAddress());
        applicant.setTelephone(updatedApplicant.getTelephone());

        return applicantRepository.save(applicant);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteApplicant(Integer id) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found"));
        applicantRepository.delete(applicant);
    }

    @PreAuthorize("hasRole('VERIFIER')")
    @Transactional
    public void verifyApplicant(Integer applicantId, String username, boolean useMakerChecker) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found"));
        User verifier = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Verifier not found"));

        if (useMakerChecker) {
            applicant.setVerificationStatus("Proposed");
            MakerCheckerLog log = new MakerCheckerLog();
            log.setEntityType("Applicant");
            log.setEntityId(applicantId);
            log.setAction("Verify");
            log.setStatus("Proposed");
            log.setMaker(verifier);
            makerCheckerLogRepository.save(log);
        } else {
            applicant.setVerificationStatus("Verified");
        }
        applicantRepository.save(applicant);

        // Optional: Use stored procedure instead
        /*
        Integer userId = userService.getUserIdByUsername(username);
        entityManager.createNativeQuery("CALL verify_applicant(:applicantId, :userId, :useMakerChecker)")
                .setParameter("applicantId", applicantId)
                .setParameter("userId", userId)
                .setParameter("useMakerChecker", useMakerChecker)
                .executeUpdate();
        */
    }

    @PreAuthorize("hasRole('APPROVER')")
    @Transactional
    public void confirmMakerChecker(Integer logId, String username, boolean approve) {
        MakerCheckerLog log = makerCheckerLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Maker-checker log not found"));
        User checker = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Checker not found"));

        if ("Applicant".equals(log.getEntityType()) && "Verify".equals(log.getAction())) {
            Applicant applicant = applicantRepository.findById(log.getEntityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Applicant not found"));
            applicant.setVerificationStatus(approve ? "Verified" : "Rejected");
            applicantRepository.save(applicant);
        }

        log.setStatus(approve ? "Approved" : "Rejected");
        log.setChecker(checker);
        makerCheckerLogRepository.save(log);

        // Optional: Use stored procedure instead
        /*
        Integer checkerId = userService.getUserIdByUsername(username);
        entityManager.createNativeQuery("CALL confirm_maker_checker(:logId, :checkerId, :approve)")
                .setParameter("logId", logId)
                .setParameter("checkerId", checkerId)
                .setParameter("approve", approve)
                .executeUpdate();
        */
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_COLLECTOR')")
    public Page<Applicant> searchApplicantsByName(String name, Pageable pageable) {
        if (name == null || name.trim().isEmpty()) {
            return applicantRepository.findAll(pageable);
        }
        return applicantRepository.findByNameContaining(name, pageable);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_COLLECTOR')")
    public Page<Applicant> searchApplicants(String name, String idNumber, LocalDate dateApplied, Pageable pageable) {
        return applicantRepository.findByFilters(name, idNumber, dateApplied, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_COLLECTOR', 'VERIFIER')")
    public Page<Applicant> filterApplicantsByStatus(String status, Pageable pageable) {
        if (status == null || status.trim().isEmpty()) {
            return applicantRepository.findAll(pageable);
        }
        return applicantRepository.findByVerificationStatus(status, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_COLLECTOR')")
    public Page<Applicant> filterApplicantsByVillage(Integer villageId, Pageable pageable) {
        if (villageId == null) {
            return applicantRepository.findAll(pageable);
        }
        return applicantRepository.findByVillageId(villageId, pageable);
    }
}
