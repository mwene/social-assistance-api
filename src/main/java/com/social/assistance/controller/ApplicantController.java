package com.social.assistance.controller;

import com.social.assistance.dto.ApplicantRequest;
import com.social.assistance.model.Applicant;
import com.social.assistance.model.Parameter;
import com.social.assistance.model.Village;
import com.social.assistance.service.ApplicantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/applicants")
@Tag(name = "Applicants", description = "Endpoints for managing applicants")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class ApplicantController {

    @Autowired
    private ApplicantService applicantService;

    @PostMapping
    @Operation(summary = "Create a new applicant", description = "Restricted to ROLE_DATA_COLLECTOR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applicant created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Duplicate ID number")
    })
    public ResponseEntity<Applicant> createApplicant(@Valid @RequestBody ApplicantRequest request) {
        Applicant applicant = mapToApplicant(request);
        Applicant createdApplicant = applicantService.createApplicant(applicant);
        return ResponseEntity.ok(createdApplicant);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get applicant by ID", description = "Retrieve an applicant by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applicant retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Applicant not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Applicant> getApplicantById(@PathVariable Integer id) {
        return applicantService.getApplicantById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all applicants", description = "Restricted to ROLE_ADMIN or ROLE_DATA_COLLECTOR, paginated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applicants retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<Applicant>> getAllApplicants(Pageable pageable) {
        Page<Applicant> applicants = applicantService.getAllApplicants(pageable);
        return ResponseEntity.ok(applicants);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an applicant", description = "Restricted to ROLE_DATA_COLLECTOR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applicant updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Applicant not found")
    })
    public ResponseEntity<Applicant> updateApplicant(@PathVariable Integer id, @Valid @RequestBody ApplicantRequest request) {
        Applicant applicant = mapToApplicant(request);
        Applicant updatedApplicant = applicantService.updateApplicant(id, applicant);
        return ResponseEntity.ok(updatedApplicant);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an applicant", description = "Restricted to ROLE_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Applicant deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Applicant not found")
    })
    public ResponseEntity<Void> deleteApplicant(@PathVariable Integer id) {
        applicantService.deleteApplicant(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/verify")
    @Operation(summary = "Verify an applicant", description = "Restricted to ROLE_VERIFIER, optional maker-checker")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applicant verification initiated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Applicant not found")
    })
    public ResponseEntity<Void> verifyApplicant(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean useMakerChecker,
            Authentication authentication) {
        applicantService.verifyApplicant(id, authentication.getName(), useMakerChecker);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/maker-checker/{logId}")
    @Operation(summary = "Confirm maker-checker action", description = "Restricted to ROLE_APPROVER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Action confirmed"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Log or entity not found")
    })
    public ResponseEntity<Void> confirmMakerChecker(
            @PathVariable Integer logId,
            @RequestParam boolean approve,
            Authentication authentication) {
        applicantService.confirmMakerChecker(logId, authentication.getName(), approve);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
@Operation(summary = "Search applicants by name, ID number, or date applied", description = "Restricted to ROLE_ADMIN or ROLE_DATA_COLLECTOR, paginated")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Applicants retrieved successfully"),
    @ApiResponse(responseCode = "403", description = "Access denied")
})
public ResponseEntity<Page<Applicant>> searchApplicants(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String idNumber,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateApplied,
        Pageable pageable) {
    Page<Applicant> applicants = applicantService.searchApplicants(name, idNumber, dateApplied, pageable);
    return ResponseEntity.ok(applicants);
}

    @GetMapping("/filter/status")
    @Operation(summary = "Filter applicants by verification status", description = "Restricted to ROLE_ADMIN, ROLE_DATA_COLLECTOR, or ROLE_VERIFIER, paginated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applicants retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<Applicant>> filterApplicantsByStatus(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<Applicant> applicants = applicantService.filterApplicantsByStatus(status, pageable);
        return ResponseEntity.ok(applicants);
    }

    @GetMapping("/filter/village")
    @Operation(summary = "Filter applicants by village", description = "Restricted to ROLE_ADMIN or ROLE_DATA_COLLECTOR, paginated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applicants retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<Applicant>> filterApplicantsByVillage(
            @RequestParam(required = false) Integer villageId,
            Pageable pageable) {
        Page<Applicant> applicants = applicantService.filterApplicantsByVillage(villageId, pageable);
        return ResponseEntity.ok(applicants);
    }

    private Applicant mapToApplicant(ApplicantRequest request) {
        Applicant applicant = new Applicant();
        applicant.setFirstName(request.getFirstName());
        applicant.setMiddleName(request.getMiddleName());
        applicant.setLastName(request.getLastName());
        Parameter sex = new Parameter();
        sex.setId(request.getSexId());
        applicant.setSex(sex);
        applicant.setAge(request.getAge());
        Parameter maritalStatus = new Parameter();
        maritalStatus.setId(request.getMaritalStatusId());
        applicant.setMaritalStatus(maritalStatus);
        applicant.setIdNumber(request.getIdNumber());
        Village village = new Village();
        village.setId(request.getVillageId()); // Set the ID directly instead of relying on new Village()
        applicant.setVillage(village);
        applicant.setPostalAddress(request.getPostalAddress());
        applicant.setPhysicalAddress(request.getPhysicalAddress());
        applicant.setTelephone(request.getTelephone());
        return applicant;
    }
}
