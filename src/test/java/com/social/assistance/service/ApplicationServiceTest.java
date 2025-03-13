package com.social.assistance.service;

import com.social.assistance.dto.ApplicationReport;
import com.social.assistance.exception.DuplicateResourceException;
import com.social.assistance.exception.InvalidStateException;
import com.social.assistance.model.Applicant;
import com.social.assistance.model.Application;
import com.social.assistance.model.MakerCheckerLog;
import com.social.assistance.model.Parameter;
import com.social.assistance.model.Programme;
import com.social.assistance.model.User;
import com.social.assistance.model.Village;
import com.social.assistance.repository.ApplicantRepository;
import com.social.assistance.repository.ApplicationRepository;
import com.social.assistance.repository.MakerCheckerLogRepository;
import com.social.assistance.repository.ProgrammeRepository;
import com.social.assistance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private ProgrammeRepository programmeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MakerCheckerLogRepository makerCheckerLogRepository;

    @InjectMocks
    private ApplicationService applicationService;

    private Applicant applicant;
    private Programme programme;

    @BeforeEach
    void setUp() {
        applicant = new Applicant();
        applicant.setId(1);
        applicant.setVerificationStatus("Verified");
        applicant.setFirstName("John");
        applicant.setLastName("Doe");
        applicant.setAge(30);
        Parameter sex = new Parameter();
        sex.setId(1);
        applicant.setSex(sex);
        Parameter maritalStatus = new Parameter();
        maritalStatus.setId(1);
        applicant.setMaritalStatus(maritalStatus);
        Village village = new Village();
        village.setId(1);
        applicant.setVillage(village);

        programme = new Programme();
        programme.setId(1);
        programme.setName("Programme A");
    }

    @Test
    void createApplication_success() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        when(applicantRepository.findById(1)).thenReturn(Optional.of(applicant));
        when(programmeRepository.findById(1)).thenReturn(Optional.of(programme));
        when(applicationRepository.findByApplicantId(1, pageable)).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Application result = applicationService.createApplication(1, 1);

        assertEquals("Pending", result.getStatus());
        assertEquals(applicant, result.getApplicant());
        assertEquals(programme, result.getProgramme());
        verify(applicationRepository, times(1)).save(any(Application.class));
    }

    @Test
    void createApplication_duplicateApplication_throwsException() {
        Application existingApp = new Application();
        existingApp.setProgramme(programme);
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);

        when(applicantRepository.findById(1)).thenReturn(Optional.of(applicant));
        when(programmeRepository.findById(1)).thenReturn(Optional.of(programme));
        when(applicationRepository.findByApplicantId(1, pageable)).thenReturn(new PageImpl<>(Collections.singletonList(existingApp)));

        assertThrows(DuplicateResourceException.class, () -> applicationService.createApplication(1, 1));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void approveApplication_withoutMakerChecker_success() {
        Application application = new Application();
        application.setApplicant(applicant);
        User approver = new User();
        approver.setUsername("approver1");

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(userRepository.findByUsername("approver1")).thenReturn(Optional.of(approver));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);

        applicationService.approveApplication(1, "approver1", false);

        assertEquals("Approved", application.getStatus());
        verify(makerCheckerLogRepository, never()).save(any());
    }

    @Test
    void approveApplication_withMakerChecker_success() {
        Application application = new Application();
        application.setApplicant(applicant);
        User approver = new User();
        approver.setUsername("approver1");

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(userRepository.findByUsername("approver1")).thenReturn(Optional.of(approver));
        when(applicationRepository.save(any(Application.class))).thenReturn(application);
        when(makerCheckerLogRepository.save(any(MakerCheckerLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        applicationService.approveApplication(1, "approver1", true);

        assertEquals("Proposed", application.getStatus());
        assertEquals(approver, application.getMaker());
        verify(makerCheckerLogRepository, times(1)).save(any());
    }

    @Test
    void approveApplication_unverifiedApplicant_throwsException() {
        Application application = new Application();
        Applicant unverifiedApplicant = new Applicant();
        unverifiedApplicant.setVerificationStatus("Pending");
        application.setApplicant(unverifiedApplicant);
        User approver = new User();
        approver.setUsername("approver1");

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(userRepository.findByUsername("approver1")).thenReturn(Optional.of(approver));

        assertThrows(InvalidStateException.class, () -> applicationService.approveApplication(1, "approver1", false));
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void getAllApplications_paginated_success() {
        Application application = new Application();
        application.setId(1);
        Page<Application> page = new PageImpl<>(Collections.singletonList(application));
        when(applicationRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Application> result = applicationService.getAllApplications(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        verify(applicationRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    void filterApplicationsByApplicantAndStatus_paginated_success() {
        Application application = new Application();
        application.setId(1);
        application.setStatus("Pending");
        Page<Application> page = new PageImpl<>(Collections.singletonList(application));
        when(applicationRepository.findByApplicantIdAndStatus(eq(1), eq("Pending"), any(PageRequest.class))).thenReturn(page);

        Page<Application> result = applicationService.filterApplicationsByApplicantAndStatus(1, "Pending", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Pending", result.getContent().get(0).getStatus());
        verify(applicationRepository, times(1)).findByApplicantIdAndStatus(eq(1), eq("Pending"), any(PageRequest.class));
    }

    @Test
    void getApplicationReport_success() {
        Object[] stats = new Object[]{10L, 5L, 3L, 2L, "Programme A"};
        when(applicationRepository.getApplicationStatsByProgramme()).thenReturn(Collections.singletonList(stats));

        List<ApplicationReport> report = applicationService.getApplicationReport();

        assertEquals(1, report.size());
        ApplicationReport result = report.get(0);
        assertEquals(10L, result.getTotalApplications());
        assertEquals(5L, result.getApprovedApplications());
        assertEquals(3L, result.getPendingApplications());
        assertEquals(2L, result.getRejectedApplications());
        assertEquals("Programme A", result.getProgrammeName());
        verify(applicationRepository, times(1)).getApplicationStatsByProgramme();
    }

    @Test
    void exportApplications_csv_success() {
        Application application = new Application();
        application.setId(1);
        application.setApplicant(applicant);
        application.setProgramme(programme);
        application.setStatus("Approved");
        application.setApplicationDate(LocalDate.of(2023, 1, 1));

        when(applicationRepository.findFilteredApplications("Approved", LocalDate.now().minusMonths(3), LocalDate.now(),
                null, null, null, "All", null)).thenReturn(Collections.singletonList(application));

        byte[] result = applicationService.exportApplications("csv", "Approved", null, null, null, null, null, "All", null, null, null, null);

        String csvContent = new String(result);
        assertTrue(csvContent.contains("ID,Applicant Name,Programme,Status,Application Date"));
        assertTrue(csvContent.contains("1,John Doe,Programme A,Approved,2023-01-01"));
        verify(applicationRepository, times(1)).findFilteredApplications("Approved", LocalDate.now().minusMonths(3), LocalDate.now(),
                null, null, null, "All", null);
    }

    @Test
    void exportApplications_excel_success() {
        Application application = new Application();
        application.setId(1);
        application.setApplicant(applicant);
        application.setProgramme(programme);
        application.setStatus("Approved");
        application.setApplicationDate(LocalDate.of(2023, 1, 1));

        when(applicationRepository.findFilteredApplications("Approved", LocalDate.now().minusMonths(3), LocalDate.now(),
                null, null, null, "All", null)).thenReturn(Collections.singletonList(application));

        byte[] result = applicationService.exportApplications("excel", "Approved", null, null, null, null, null, "All", null, null, null, null);

        assertNotNull(result);
        assertTrue(result.length > 0); // Basic check for non-empty Excel file
        verify(applicationRepository, times(1)).findFilteredApplications("Approved", LocalDate.now().minusMonths(3), LocalDate.now(),
                null, null, null, "All", null);
    }

    @Test
    void exportApplications_pdf_success() {
        Application application = new Application();
        application.setId(1);
        application.setApplicant(applicant);
        application.setProgramme(programme);
        application.setStatus("Approved");
        application.setApplicationDate(LocalDate.of(2023, 1, 1));

        when(applicationRepository.findFilteredApplications("Approved", LocalDate.now().minusMonths(3), LocalDate.now(),
                null, null, null, "All", null)).thenReturn(Collections.singletonList(application));

        byte[] result = applicationService.exportApplications("pdf", "Approved", null, null, null, null, null, "All", null,
                "Test Org", null, "123 Test St");

        assertNotNull(result);
        assertTrue(result.length > 0); // Basic check for non-empty PDF file
        verify(applicationRepository, times(1)).findFilteredApplications("Approved", LocalDate.now().minusMonths(3), LocalDate.now(),
                null, null, null, "All", null);
    }

    @Test
    void exportApplications_invalidFormat_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            applicationService.exportApplications("invalid", "all", null, null, null, null, null, "All", null, null, null, null));
        verify(applicationRepository, never()).findFilteredApplications(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
