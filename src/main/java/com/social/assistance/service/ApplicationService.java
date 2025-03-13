package com.social.assistance.service;

import com.social.assistance.dto.ApplicationReport;
import com.social.assistance.exception.DuplicateResourceException;
import com.social.assistance.exception.InvalidStateException;
import com.social.assistance.exception.ResourceNotFoundException;
import com.social.assistance.model.Applicant;
import com.social.assistance.model.Application;
import com.social.assistance.model.MakerCheckerLog;
import com.social.assistance.model.Programme;
import com.social.assistance.model.User;
import com.social.assistance.repository.ApplicantRepository;
import com.social.assistance.repository.ApplicationRepository;
import com.social.assistance.repository.MakerCheckerLogRepository;
import com.social.assistance.repository.ProgrammeRepository;
import com.social.assistance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final ProgrammeRepository programmeRepository;
    private final UserRepository userRepository;
    private final MakerCheckerLogRepository makerCheckerLogRepository;
    private final UserService userService; // Added for username-to-ID lookup

    @PersistenceContext
    private EntityManager entityManager; // For stored procedures (optional)

    @PreAuthorize("hasRole('DATA_COLLECTOR')")
    @Transactional
    public Application createApplication(Integer applicantId, Integer programmeId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found with ID: " + applicantId));
        Programme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found with ID: " + programmeId));

        Page<Application> existingApplications = applicationRepository.findByApplicantId(applicantId, PageRequest.of(0, Integer.MAX_VALUE));
        if (existingApplications.getContent().stream().anyMatch(app -> app.getProgramme().getId().equals(programmeId))) {
            throw new DuplicateResourceException("Application already exists for this applicant and programme");
        }

        Application application = new Application();
        application.setApplicant(applicant);
        application.setProgramme(programme);
        application.setStatus("Pending");

        return applicationRepository.save(application);
    }

    public Optional<Application> getApplicationById(Integer id) {
        return applicationRepository.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_COLLECTOR')")
    public Page<Application> getAllApplications(Pageable pageable) {
        return applicationRepository.findAll(pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_COLLECTOR')")
    @Transactional
    public Application updateApplication(Integer id, Integer applicantId, Integer programmeId) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));

        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found with ID: " + applicantId));
        Programme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme not found with ID: " + programmeId));

        Page<Application> existingApplications = applicationRepository.findByApplicantId(applicantId, PageRequest.of(0, Integer.MAX_VALUE));
        if (existingApplications.getContent().stream()
                .anyMatch(app -> app.getProgramme().getId().equals(programmeId) && !app.getId().equals(id))) {
            throw new DuplicateResourceException("Another application already exists for this applicant and programme");
        }

        application.setApplicant(applicant);
        application.setProgramme(programme);

        return applicationRepository.save(application);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteApplication(Integer id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
        applicationRepository.delete(application);
    }

    @PreAuthorize("hasRole('APPROVER')")
    @Transactional
    public void approveApplication(Integer id, String username, boolean useMakerChecker) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
        User approver = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Approver not found with username: " + username));

        if (!"Verified".equals(application.getApplicant().getVerificationStatus())) {
            throw new InvalidStateException("Cannot approve application: Applicant is not verified");
        }

        if (useMakerChecker) {
            application.setStatus("Proposed");
            application.setMaker(approver);
            MakerCheckerLog log = new MakerCheckerLog();
            log.setEntityType("Application");
            log.setEntityId(id);
            log.setAction("Approve");
            log.setStatus("Proposed");
            log.setMaker(approver);
            makerCheckerLogRepository.save(log);
        } else {
            application.setStatus("Approved");
        }
        applicationRepository.save(application);

        // Optional: Use stored procedure instead
        /*
        Integer userId = userService.getUserIdByUsername(username);
        entityManager.createNativeQuery("CALL approve_application(:applicationId, :userId, :useMakerChecker)")
                .setParameter("applicationId", id)
                .setParameter("userId", userId)
                .setParameter("useMakerChecker", useMakerChecker)
                .executeUpdate();
        */
    }

    @PreAuthorize("hasRole('APPROVER')")
    @Transactional
    public void confirmMakerChecker(Integer logId, String username, boolean approve) {
        MakerCheckerLog log = makerCheckerLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Maker-checker log not found with ID: " + logId));
        User checker = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Checker not found with username: " + username));

        if ("Application".equals(log.getEntityType()) && "Approve".equals(log.getAction())) {
            Application application = applicationRepository.findById(log.getEntityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + log.getEntityId()));
            application.setStatus(approve ? "Approved" : "Rejected");
            applicationRepository.save(application);
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
    public Page<Application> getApplicationsByStatus(String status, Pageable pageable) {
        return applicationRepository.findByStatus(status, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_COLLECTOR', 'APPROVER')")
    public Page<Application> filterApplicationsByApplicantAndStatus(Integer applicantId, String status, Pageable pageable) {
        if (applicantId == null && (status == null || status.trim().isEmpty())) {
            return applicationRepository.findAll(pageable);
        }
        if (applicantId == null) {
            return applicationRepository.findByStatus(status, pageable);
        }
        if (status == null || status.trim().isEmpty()) {
            return applicationRepository.findByApplicantId(applicantId, pageable);
        }
        return applicationRepository.findByApplicantIdAndStatus(applicantId, status, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DATA_COLLECTOR')")
    public Page<Application> filterApplicationsByProgramme(Integer programmeId, Pageable pageable) {
        if (programmeId == null) {
            return applicationRepository.findAll(pageable);
        }
        return applicationRepository.findByProgrammeId(programmeId, pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<ApplicationReport> getApplicationReport() {
        List<Object[]> results = applicationRepository.getApplicationStatsByProgramme();
        return results.stream().map(result -> new ApplicationReport(
            ((Number) result[0]).longValue(), // total
            ((Number) result[1]).longValue(), // approved
            ((Number) result[2]).longValue(), // pending
            ((Number) result[3]).longValue(), // rejected
            (String) result[4]                // programmeName
        )).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public byte[] exportApplications(String format, String status, LocalDate startDate, LocalDate endDate,
                                     Integer age, Integer sexId, Integer maritalStatusId, String physicalLocationLevel, Integer physicalLocationId,
                                     String orgName, String logoPath, String orgAddress) {
        // Default to last 3 months if dates not provided
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStartDate = startDate != null ? startDate : effectiveEndDate.minusMonths(3);

        // Fetch filtered applications
        List<Application> applications = applicationRepository.findFilteredApplications(
            status == null || "all".equalsIgnoreCase(status) ? null : status,
            effectiveStartDate, effectiveEndDate,
            age, sexId, maritalStatusId,
            physicalLocationLevel == null || "All".equalsIgnoreCase(physicalLocationLevel) ? "All" : physicalLocationLevel,
            physicalLocationId
        );

        // Export based on format
        switch (format.toLowerCase()) {
            case "csv":
                return exportToCsv(applications);
            case "excel":
                return exportToExcel(applications);
            case "pdf":
                return exportToPdf(applications, orgName, logoPath, orgAddress);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private byte[] exportToCsv(List<Application> applications) {
        try (StringWriter stringWriter = new StringWriter();
             CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            String[] header = {"ID", "Applicant Name", "Programme", "Status", "Application Date"};
            csvWriter.writeNext(header);

            for (Application app : applications) {
                String applicantName = app.getApplicant().getFirstName() + " " +
                                      (app.getApplicant().getMiddleName() != null ? app.getApplicant().getMiddleName() + " " : "") +
                                      app.getApplicant().getLastName();
                String[] row = {
                    String.valueOf(app.getId()),
                    applicantName,
                    app.getProgramme().getName(),
                    app.getStatus(),
                    app.getApplicationDate().toString()
                };
                csvWriter.writeNext(row);
            }
            return stringWriter.toString().getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export to CSV", e);
        }
    }

    private byte[] exportToExcel(List<Application> applications) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Applications");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Applicant Name", "Programme", "Status", "Application Date"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (Application app : applications) {
                String applicantName = app.getApplicant().getFirstName() + " " +
                                      (app.getApplicant().getMiddleName() != null ? app.getApplicant().getMiddleName() + " " : "") +
                                      app.getApplicant().getLastName();
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(app.getId());
                row.createCell(1).setCellValue(applicantName);
                row.createCell(2).setCellValue(app.getProgramme().getName());
                row.createCell(3).setCellValue(app.getStatus());
                row.createCell(4).setCellValue(app.getApplicationDate().toString());
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export to Excel", e);
        }
    }

    private byte[] exportToPdf(List<Application> applications, String orgName, String logoPath, String orgAddress) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Header
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            if (orgName != null && !orgName.trim().isEmpty()) {
                Paragraph orgNamePara = new Paragraph(orgName, headerFont);
                orgNamePara.setAlignment(Element.ALIGN_CENTER);
                document.add(orgNamePara);
            }
            if (logoPath != null && !logoPath.trim().isEmpty()) {
                try {
                    Image logo = Image.getInstance(logoPath);
                    logo.scaleToFit(100, 100);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    document.add(logo);
                } catch (Exception e) {
                    System.err.println("Failed to load logo: " + e.getMessage());
                }
            }
            if (orgAddress != null && !orgAddress.trim().isEmpty()) {
                Paragraph addressPara = new Paragraph(orgAddress);
                addressPara.setAlignment(Element.ALIGN_CENTER);
                document.add(addressPara);
            }
            document.add(new Paragraph(" ")); // Spacer

            // Table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            String[] headers = {"ID", "Applicant Name", "Programme", "Status", "Application Date"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            for (Application app : applications) {
                String applicantName = app.getApplicant().getFirstName() + " " +
                                      (app.getApplicant().getMiddleName() != null ? app.getApplicant().getMiddleName() + " " : "") +
                                      app.getApplicant().getLastName();
                table.addCell(String.valueOf(app.getId()));
                table.addCell(applicantName);
                table.addCell(app.getProgramme().getName());
                table.addCell(app.getStatus());
                table.addCell(app.getApplicationDate().toString());
            }
            document.add(table);

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to export to PDF", e);
        }
    }
}
