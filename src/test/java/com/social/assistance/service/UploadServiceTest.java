package com.social.assistance.service;

import com.social.assistance.exception.InvalidFileException;
import com.social.assistance.model.*;
import com.social.assistance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UploadServiceTest {

    @InjectMocks
    private UploadService uploadService;

    @Mock
    private ApplicantRepository applicantRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ProgrammeRepository programmeRepository;

    @Mock
    private ParameterRepository parameterRepository;

    @Mock
    private VillageRepository villageRepository;

    @Mock
    private SubLocationRepository subLocationRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private SubCountyRepository subCountyRepository;

    @Mock
    private CountyRepository countyRepository;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUploadApplicants_CSV_Success() {
        String csvContent = "firstName,middleName,lastName,sexId,age,maritalStatusId,idNumber,villageId,postalAddress,physicalAddress,telephone\n" +
                "John,,Doe,1,30,2,12345678,1,PO Box 123,Main St,0712345678";
        MultipartFile file = new MockMultipartFile("file", "applicants.csv", "text/csv", csvContent.getBytes());

        Parameter gender =  new Parameter();
        gender.setId(1);
        gender.setCategory("Sex");
        gender.setValue("Male");
        Parameter maritalStatus =  new Parameter();
        maritalStatus.setId(2);
        maritalStatus.setCategory("Marital Status");
        maritalStatus.setValue("Single");
        when(parameterRepository.findById(1)).thenReturn(Optional.of(gender));
        when(parameterRepository.findById(2)).thenReturn(Optional.of(maritalStatus));
        when(villageRepository.findById(1)).thenReturn(Optional.of(new Village()));
        when(applicantRepository.saveAll(any())).thenReturn(Collections.singletonList(new Applicant()));

        List<Applicant> result = uploadService.uploadApplicants(file);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(applicantRepository, times(1)).saveAll(any());
    }

    @Test
    void testUploadApplicants_InvalidFile() {
        MultipartFile file = new MockMultipartFile("file", "applicants.txt", "text/plain", "".getBytes());

        assertThrows(InvalidFileException.class, () -> uploadService.uploadApplicants(file));
    }

    @Test
    void testUploadApplications_CSV_Success() {
        String csvContent = "firstName,middleName,lastName,programmeName,applicationDate\n" +
                "John,,Doe,Health Program,2023-01-01";
        MultipartFile file = new MockMultipartFile("file", "applications.csv", "text/csv", csvContent.getBytes());

        Applicant applicant = new Applicant();
        Programme programme = new Programme();
        when(applicantRepository.findByFirstNameAndLastNameAndMiddleName("John", "Doe", null)).thenReturn(Optional.of(applicant));
        when(programmeRepository.findByName("Health Program")).thenReturn(Optional.of(programme));
        when(applicationRepository.saveAll(any())).thenReturn(Collections.singletonList(new Application()));

        List<Application> result = uploadService.uploadApplications(file);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(applicationRepository, times(1)).saveAll(any());
    }

    @Test
    void testUploadParameters_CSV_Success() {
        String csvContent = "category,value\n" +
                "Sex,Male";
        MultipartFile file = new MockMultipartFile("file", "parameters.csv", "text/csv", csvContent.getBytes());

        when(parameterRepository.saveAll(any())).thenReturn(Collections.singletonList(new Parameter()));

        List<Parameter> result = uploadService.uploadParameters(file);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(parameterRepository, times(1)).saveAll(any());
    }

    @Test
    void testUploadPhysicalLocations_CSV_Success() {
        String csvContent = "countyName,subCountyName,locationName,subLocationName,villageName\n" +
                "Nairobi,Westlands,Lavington,Kilimani,Hurlingham";
        MultipartFile file = new MockMultipartFile("file", "locations.csv", "text/csv", csvContent.getBytes());

        when(countyRepository.findByName(any())).thenReturn(Optional.empty());
        when(subCountyRepository.findByNameAndCounty(any(), any())).thenReturn(Optional.empty());
        when(locationRepository.findByNameAndSubCounty(any(), any())).thenReturn(Optional.empty());
        when(subLocationRepository.findByNameAndLocation(any(), any())).thenReturn(Optional.empty());
        when(villageRepository.findByNameAndSubLocation(any(), any())).thenReturn(Optional.empty());
        when(countyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subCountyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(locationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subLocationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(villageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> uploadService.uploadPhysicalLocations(file));
        verify(countyRepository, times(1)).save(any());
    }

    @Test
    void testUploadUsers_CSV_Success() {
        String csvContent = "username,password,name,role,email,phone\n" +
                "jdoe,password123,John Doe,ADMIN,john@example.com,0712345678";
        MultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", csvContent.getBytes());

        when(userRepository.saveAll(any())).thenReturn(Collections.singletonList(new User()));

        List<User> result = uploadService.uploadUsers(file);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(userRepository, times(1)).saveAll(any());
    }

    @Test
    void testUploadApplicantImage_Success() {
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "image data".getBytes());
        Applicant applicant = new Applicant();
        when(applicantRepository.findById(1)).thenReturn(Optional.of(applicant));
        when(applicantRepository.save(any())).thenReturn(applicant);

        String result = uploadService.uploadApplicantImage(1, image);

        assertNotNull(result);
        assertTrue(result.contains("images/1/"));
        verify(applicantRepository, times(1)).save(applicant);
    }
}
