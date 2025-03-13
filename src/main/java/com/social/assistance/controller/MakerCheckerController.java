package com.social.assistance.controller;

import com.social.assistance.dto.ApplicantRequest;
import com.social.assistance.dto.UserRegistrationRequest;
import com.social.assistance.model.Applicant;
import com.social.assistance.model.Application;
import com.social.assistance.model.MakerCheckerLog;
import com.social.assistance.model.User;
import com.social.assistance.repository.ApplicantRepository;
import com.social.assistance.repository.ApplicationRepository;
import com.social.assistance.repository.MakerCheckerLogRepository;
import com.social.assistance.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@RestController
@RequestMapping("/api/maker-checker")
@Tag(name = "Maker-Checker", description = "Endpoints for maker-checker workflow and user management")
public class MakerCheckerController {

    private final ApplicantRepository applicantRepository;
    private final ApplicationRepository applicationRepository;
    private final MakerCheckerLogRepository makerCheckerLogRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public MakerCheckerController(ApplicantRepository applicantRepository,
                                  ApplicationRepository applicationRepository,
                                  MakerCheckerLogRepository makerCheckerLogRepository,
                                  UserRepository userRepository) {
        this.applicantRepository = applicantRepository;
        this.applicationRepository = applicationRepository;
        this.makerCheckerLogRepository = makerCheckerLogRepository;
        this.userRepository = userRepository;
    }
    
    @PostMapping("/applications")
    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    @Operation(summary = "Insert a new application", description = "Calls insert_application stored procedure")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application inserted"),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @Transactional
    public ResponseEntity<String> insertApplication(@RequestBody ApplicantRequest request) {
        entityManager.createNativeQuery(
                "CALL insert_application(:firstName, :middleName, :lastName, :sexId, :age, :maritalStatusId, " +
                ":idNumber, :villageId, :postalAddress, :physicalAddress, :telephone, :programmeId)")
                .setParameter("firstName", request.getFirstName())
                .setParameter("middleName", request.getMiddleName())
                .setParameter("lastName", request.getLastName())
                .setParameter("sexId", request.getSexId())
                .setParameter("age", request.getAge())
                .setParameter("maritalStatusId", request.getMaritalStatusId())
                .setParameter("idNumber", request.getIdNumber())
                .setParameter("villageId", request.getVillageId())
                .setParameter("postalAddress", request.getPostalAddress())
                .setParameter("physicalAddress", request.getPhysicalAddress())
                .setParameter("telephone", request.getTelephone())
                .setParameter("programmeId", request.getProgrammeId())
                .executeUpdate();

        return ResponseEntity.ok("Application inserted for " + request.getFirstName() + " " + request.getLastName());
    }
    
    @PostMapping("/users/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new user", description = "Calls register_user stored procedure")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered"),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    @Transactional
    public ResponseEntity<String> registerUser(@RequestBody UserRegistrationRequest request) {
        entityManager.createNativeQuery("CALL register_user(:username, :password, :name, :role)")
                .setParameter("username", request.getUsername())
                .setParameter("password", request.getPassword()) // Note: Should be encoded in production
                .setParameter("name", request.getName())
                .setParameter("role", request.getRole())
                .executeUpdate();

        return ResponseEntity.ok("User registered: " + request.getUsername());
    }
    
    @PutMapping("/users/{id}/password")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #username")
    @Operation(summary = "Change user password", description = "Calls change_user_password stored procedure")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @Transactional
    public ResponseEntity<String> changeUserPassword(
            @PathVariable Integer id,
            @RequestBody String newPassword,
            Authentication authentication) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        String username = user.getUsername();

        entityManager.createNativeQuery("CALL change_user_password(:userId, :newPassword)")
                .setParameter("userId", id)
                .setParameter("newPassword", newPassword) // Note: Should be encoded in production
                .executeUpdate();

        return ResponseEntity.ok("Password changed for user: " + username);
    }
    

    @PostMapping("/applicants/{id}/verify")
    @PreAuthorize("hasRole('VERIFIER')")
    @Operation(summary = "Verify an applicant", description = "Calls verify_applicant stored procedure")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verification initiated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Applicant not found")
    })
    @Transactional
    public ResponseEntity<String> verifyApplicant(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean useMakerChecker,
            Authentication authentication) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Applicant not found: " + id));
        Integer userId = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();

        entityManager.createNativeQuery("CALL verify_applicant(:applicantId, :userId, :useMakerChecker)")
                .setParameter("applicantId", id)
                .setParameter("userId", userId)
                .setParameter("useMakerChecker", useMakerChecker)
                .executeUpdate();

        return ResponseEntity.ok("Verification " + (useMakerChecker ? "proposed" : "completed") + " for applicant " + id);
    }

    @PostMapping("/applications/{id}/approve")
    @PreAuthorize("hasRole('APPROVER')")
    @Operation(summary = "Approve an application", description = "Calls approve_application stored procedure")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Approval initiated"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    @Transactional
    public ResponseEntity<String> approveApplication(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean useMakerChecker,
            Authentication authentication) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found: " + id));
        Integer userId = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();

        entityManager.createNativeQuery("CALL approve_application(:applicationId, :userId, :useMakerChecker)")
                .setParameter("applicationId", id)
                .setParameter("userId", userId)
                .setParameter("useMakerChecker", useMakerChecker)
                .executeUpdate();

        return ResponseEntity.ok("Approval " + (useMakerChecker ? "proposed" : "completed") + " for application " + id);
    }

    @PostMapping("/logs/{logId}/confirm")
    @PreAuthorize("hasAnyRole('APPROVER', 'VERIFIER')")
    @Operation(summary = "Confirm or reject a maker-checker action", description = "Calls confirm_maker_checker stored procedure")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Action confirmed/rejected"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Log not found")
    })
    @Transactional
    public ResponseEntity<String> confirmMakerChecker(
            @PathVariable Integer logId,
            @RequestParam boolean approve,
            Authentication authentication) {
        MakerCheckerLog log = makerCheckerLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found: " + logId));
        Integer userId = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();

        entityManager.createNativeQuery("CALL confirm_maker_checker(:logId, :checkerId, :approve)")
                .setParameter("logId", logId)
                .setParameter("checkerId", userId)
                .setParameter("approve", approve)
                .executeUpdate();

        return ResponseEntity.ok("Log " + logId + " " + (approve ? "approved" : "rejected"));
    }

    @GetMapping("/logs/pending")
    @PreAuthorize("hasAnyRole('APPROVER', 'VERIFIER')")
    @Operation(summary = "Get pending maker-checker logs", description = "Lists logs with status 'Proposed'")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of pending logs"),
            @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    public ResponseEntity<List<MakerCheckerLog>> getPendingLogs() {
        List<MakerCheckerLog> pendingLogs = makerCheckerLogRepository.findByStatus("Proposed");
        return ResponseEntity.ok(pendingLogs);
    }
}
