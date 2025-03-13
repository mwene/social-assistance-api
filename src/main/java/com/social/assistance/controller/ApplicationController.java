package com.social.assistance.controller;

import com.social.assistance.dto.ApplicationReport;
import com.social.assistance.dto.ApplicationRequest;
import com.social.assistance.model.Application;
import com.social.assistance.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Endpoints for managing applications")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @Operation(summary = "Create a new application", description = "Restricted to ROLE_DATA_COLLECTOR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Applicant or Programme not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate application")
    })
    public ResponseEntity<Application> createApplication(@Valid @RequestBody ApplicationRequest request) {
        Application application = applicationService.createApplication(request.getApplicantId(), request.getProgrammeId());
        return ResponseEntity.ok(application);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application by ID", description = "Retrieve an application by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Application> getApplicationById(@PathVariable Integer id) {
        return applicationService.getApplicationById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all applications", description = "Restricted to ROLE_ADMIN or ROLE_DATA_COLLECTOR, paginated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<Application>> getAllApplications(Pageable pageable) {
        Page<Application> applications = applicationService.getAllApplications(pageable);
        return ResponseEntity.ok(applications);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an application", description = "Restricted to ROLE_ADMIN or ROLE_DATA_COLLECTOR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Application, Applicant, or Programme not found")
    })
    public ResponseEntity<Application> updateApplication(
            @PathVariable Integer id,
            @Valid @RequestBody ApplicationRequest request) {
        Application updatedApplication = applicationService.updateApplication(id, request.getApplicantId(), request.getProgrammeId());
        return ResponseEntity.ok(updatedApplication);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an application", description = "Restricted to ROLE_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Application deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<Void> deleteApplication(@PathVariable Integer id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve an application", description = "Restricted to ROLE_APPROVER, optional maker-checker")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application approval initiated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Application not found"),
            @ApiResponse(responseCode = "400", description = "Invalid state (applicant not verified)")
    })
    public ResponseEntity<Void> approveApplication(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean useMakerChecker,
            Authentication authentication) {
        applicationService.approveApplication(id, authentication.getName(), useMakerChecker);
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
        applicationService.confirmMakerChecker(logId, authentication.getName(), approve);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/filter/status")
    @Operation(summary = "Filter applications by status", description = "Restricted to ROLE_ADMIN or ROLE_DATA_COLLECTOR, paginated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<Application>> getApplicationsByStatus(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<Application> applications = applicationService.getApplicationsByStatus(status, pageable);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/filter/applicant-status")
    @Operation(summary = "Filter applications by applicant and status", description = "Restricted to ROLE_ADMIN, ROLE_DATA_COLLECTOR, or ROLE_APPROVER, paginated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<Application>> filterApplicationsByApplicantAndStatus(
            @RequestParam(required = false) Integer applicantId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<Application> applications = applicationService.filterApplicationsByApplicantAndStatus(applicantId, status, pageable);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/filter/programme")
    @Operation(summary = "Filter applications by programme", description = "Restricted to ROLE_ADMIN or ROLE_DATA_COLLECTOR, paginated")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applications retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Page<Application>> filterApplicationsByProgramme(
            @RequestParam(required = false) Integer programmeId,
            Pageable pageable) {
        Page<Application> applications = applicationService.filterApplicationsByProgramme(programmeId, pageable);
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/report")
    @Operation(summary = "Get application report", description = "Restricted to ROLE_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<List<ApplicationReport>> getApplicationReport() {
        List<ApplicationReport> report = applicationService.getApplicationReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/export")
    @Operation(summary = "Export applications to CSV, Excel, or PDF", description = "Restricted to ROLE_ADMIN, filterable by status, date, and applicant parameters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export successful"),
            @ApiResponse(responseCode = "400", description = "Invalid format or parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<byte[]> exportApplications(
            @RequestParam String format,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) Integer sexId,
            @RequestParam(required = false) Integer maritalStatusId,
            @RequestParam(defaultValue = "All") String physicalLocationLevel,
            @RequestParam(required = false) Integer physicalLocationId,
            @RequestParam(required = false) String orgName,
            @RequestParam(required = false) String logoPath,
            @RequestParam(required = false) String orgAddress) {
        
        byte[] fileContent = applicationService.exportApplications(
            format, status, startDate, endDate, age, sexId, maritalStatusId, physicalLocationLevel, physicalLocationId,
            orgName, logoPath, orgAddress
        );

        String contentType;
        String fileExtension;
        switch (format.toLowerCase()) {
            case "csv":
                contentType = "text/csv";
                fileExtension = ".csv";
                break;
            case "excel":
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                fileExtension = ".xlsx";
                break;
            case "pdf":
                contentType = "application/pdf";
                fileExtension = ".pdf";
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", "applications_export" + fileExtension);
        headers.setContentLength(fileContent.length);

        return new ResponseEntity<>(fileContent, headers, org.springframework.http.HttpStatus.OK);
    }
}
