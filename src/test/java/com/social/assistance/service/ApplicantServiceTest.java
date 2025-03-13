package com.social.assistance.service;

import com.social.assistance.exception.DuplicateResourceException;
import com.social.assistance.model.Applicant;
import com.social.assistance.model.Parameter;
import com.social.assistance.model.User;
import com.social.assistance.model.Village;
import com.social.assistance.repository.ApplicantRepository;
import com.social.assistance.repository.MakerCheckerLogRepository;
import com.social.assistance.repository.ParameterRepository;
import com.social.assistance.repository.UserRepository;
import com.social.assistance.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicantServiceTest {

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MakerCheckerLogRepository makerCheckerLogRepository;

    @Mock
    private ParameterRepository parameterRepository;

    @Mock
    private VillageRepository villageRepository;

    @InjectMocks
    private ApplicantService applicantService;

    private Applicant applicant;

    @BeforeEach
    void setUp() {
        applicant = new Applicant();
        applicant.setIdNumber("12345678");
        applicant.setFirstName("John");
        applicant.setLastName("Doe");
        Parameter sex = new Parameter();
        sex.setId(1);
        applicant.setSex(sex);
        applicant.setAge(30);
        Parameter maritalStatus = new Parameter();
        maritalStatus.setId(1);
        applicant.setMaritalStatus(maritalStatus);
        Village village = new Village();
        village.setId(1);
        applicant.setVillage(village);
    }

    @Test
    void createApplicant_success() {
        when(applicantRepository.existsByIdNumber("12345678")).thenReturn(false);
        when(parameterRepository.findById(1)).thenReturn(Optional.of(new Parameter()));
        when(villageRepository.findById(1)).thenReturn(Optional.of(new Village()));
        when(applicantRepository.save(any(Applicant.class))).thenReturn(applicant);

        Applicant result = applicantService.createApplicant(applicant);

        assertEquals("Pending", result.getVerificationStatus());
        verify(applicantRepository, times(1)).save(applicant);
    }

    @Test
    void createApplicant_duplicateIdNumber_throwsException() {
        when(applicantRepository.existsByIdNumber("12345678")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> applicantService.createApplicant(applicant));
        verify(applicantRepository, never()).save(any(Applicant.class));
    }

    @Test
    void verifyApplicant_withoutMakerChecker_success() {
        User verifier = new User();
        verifier.setUsername("verifier1");

        when(applicantRepository.findById(1)).thenReturn(Optional.of(applicant));
        when(userRepository.findByUsername("verifier1")).thenReturn(Optional.of(verifier));
        when(applicantRepository.save(any(Applicant.class))).thenReturn(applicant);

        applicantService.verifyApplicant(1, "verifier1", false);

        assertEquals("Verified", applicant.getVerificationStatus());
        verify(makerCheckerLogRepository, never()).save(any());
    }

    @Test
    void verifyApplicant_withMakerChecker_success() {
        User verifier = new User();
        verifier.setUsername("verifier1");

        when(applicantRepository.findById(1)).thenReturn(Optional.of(applicant));
        when(userRepository.findByUsername("verifier1")).thenReturn(Optional.of(verifier));
        when(applicantRepository.save(any(Applicant.class))).thenReturn(applicant);

        applicantService.verifyApplicant(1, "verifier1", true);

        assertEquals("Proposed", applicant.getVerificationStatus());
        verify(makerCheckerLogRepository, times(1)).save(any());
    }

    @Test
    void getAllApplicants_paginated_success() {
        Page<Applicant> page = new PageImpl<>(Collections.singletonList(applicant));
        when(applicantRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Applicant> result = applicantService.getAllApplicants(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        verify(applicantRepository, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    void searchApplicantsByName_paginated_success() {
        Page<Applicant> page = new PageImpl<>(Collections.singletonList(applicant));
        when(applicantRepository.findByNameContaining(eq("john"), any(PageRequest.class))).thenReturn(page);

        Page<Applicant> result = applicantService.searchApplicantsByName("john", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("John", result.getContent().get(0).getFirstName());
        verify(applicantRepository, times(1)).findByNameContaining(eq("john"), any(PageRequest.class));
    }

    @Test
    void filterApplicantsByStatus_success() {
        Applicant verifiedApplicant = new Applicant();
        verifiedApplicant.setVerificationStatus("Verified");
        Page<Applicant> page = new PageImpl<>(Collections.singletonList(verifiedApplicant));

        when(applicantRepository.findByVerificationStatus(eq("Verified"), any(PageRequest.class))).thenReturn(page);

        Page<Applicant> result = applicantService.filterApplicantsByStatus("Verified", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Verified", result.getContent().get(0).getVerificationStatus());
        verify(applicantRepository, times(1)).findByVerificationStatus(eq("Verified"), any(PageRequest.class));
    }
}
