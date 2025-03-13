package com.social.assistance.controller;

import com.social.assistance.exception.InvalidFileException;
import com.social.assistance.model.Applicant;
import com.social.assistance.model.Application;
import com.social.assistance.model.Parameter;
import com.social.assistance.model.User;
import com.social.assistance.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/applicants")
    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    public ResponseEntity<?> uploadApplicants(@RequestParam("file") MultipartFile file) {
        try {
            List<Applicant> applicants = uploadService.uploadApplicants(file);
            return ResponseEntity.ok(applicants);
        } catch (InvalidFileException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @PostMapping("/applications")
    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    public ResponseEntity<?> uploadApplications(@RequestParam("file") MultipartFile file) {
        try {
            List<Application> applications = uploadService.uploadApplications(file);
            return ResponseEntity.ok(applications);
        } catch (InvalidFileException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @PostMapping("/parameters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadParameters(@RequestParam("file") MultipartFile file) {
        try {
            List<Parameter> parameters = uploadService.uploadParameters(file);
            return ResponseEntity.ok(parameters);
        } catch (InvalidFileException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @PostMapping("/locations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadPhysicalLocations(@RequestParam("file") MultipartFile file) {
        try {
            uploadService.uploadPhysicalLocations(file);
            return ResponseEntity.ok("Physical locations uploaded successfully");
        } catch (InvalidFileException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> uploadUsers(@RequestParam("file") MultipartFile file) {
        try {
            List<User> users = uploadService.uploadUsers(file);
            return ResponseEntity.ok(users);
        } catch (InvalidFileException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/applicants/{applicantId}/image")
    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    public ResponseEntity<String> uploadApplicantImage(
            @PathVariable Integer applicantId,
            @RequestParam("image") MultipartFile image) {
        try {
            String filePath = uploadService.uploadApplicantImage(applicantId, image);
            return ResponseEntity.ok("Image uploaded successfully: " + filePath);
        } catch (InvalidFileException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid image: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error: " + e.getMessage());
        }
    }

    // Template Download Endpoints
    @GetMapping("/templates/applicants")
    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    public ResponseEntity<byte[]> downloadApplicantsTemplate(@RequestParam("format") String format) {
        return generateTemplate(
                format,
                "applicants_template",
                "firstName,middleName,lastName,sexId,age,maritalStatusId,idNumber,villageId,postalAddress,physicalAddress,telephone"
        );
    }

    @GetMapping("/templates/applications")
    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    public ResponseEntity<byte[]> downloadApplicationsTemplate(@RequestParam("format") String format) {
        return generateTemplate(
                format,
                "applications_template",
                "firstName,middleName,lastName,programmeName,applicationDate"
        );
    }

    @GetMapping("/templates/parameters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadParametersTemplate(@RequestParam("format") String format) {
        return generateTemplate(
                format,
                "parameters_template",
                "category,value"
        );
    }

    @GetMapping("/templates/locations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadPhysicalLocationsTemplate(@RequestParam("format") String format) {
        return generateTemplate(
                format,
                "locations_template",
                "countyName,subCountyName,locationName,subLocationName,villageName"
        );
    }

    @GetMapping("/templates/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadUsersTemplate(@RequestParam("format") String format) {
        return generateTemplate(
                format,
                "users_template",
                "username,password,name,role,email,phone"
        );
    }

    // Helper method to generate CSV or Excel template
    private ResponseEntity<byte[]> generateTemplate(String format, String fileName, String headers) {
        try {
            byte[] content;
            String contentType;
            String extension;

            if ("csv".equalsIgnoreCase(format)) {
                // Generate CSV
                content = headers.getBytes();
                contentType = "text/csv";
                extension = ".csv";
            } else if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
                // Generate Excel
                try (Workbook workbook = new XSSFWorkbook()) {
                    Sheet sheet = workbook.createSheet("Template");
                    Row headerRow = sheet.createRow(0);
                    String[] headerArray = headers.split(",");
                    for (int i = 0; i < headerArray.length; i++) {
                        headerRow.createCell(i).setCellValue(headerArray[i]);
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    workbook.write(baos);
                    content = baos.toByteArray();
                }
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                extension = ".xlsx";
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid format. Use 'csv' or 'excel'.".getBytes());
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName + extension);
            responseHeaders.setContentType(MediaType.parseMediaType(contentType));
            return new ResponseEntity<>(content, responseHeaders, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Failed to generate template: " + e.getMessage()).getBytes());
        }
    }
}
