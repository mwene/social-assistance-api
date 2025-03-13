package com.social.assistance.controller;

import com.social.assistance.exception.InvalidFileException;
import com.social.assistance.model.Applicant;
import com.social.assistance.service.UploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UploadControllerTest {

    @InjectMocks
    private UploadController uploadController;

    @Mock
    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUploadApplicants_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "applicants.csv", "text/csv", "data".getBytes());
        when(uploadService.uploadApplicants(file)).thenReturn(Collections.singletonList(new Applicant()));

        ResponseEntity<?> response = uploadController.uploadApplicants(file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(Objects.requireNonNull(response.getBody()));
        verify(uploadService, times(1)).uploadApplicants(file);
    }

    @Test
    void testUploadApplicants_InvalidFile() {
        MockMultipartFile file = new MockMultipartFile("file", "applicants.txt", "text/plain", "data".getBytes());
        when(uploadService.uploadApplicants(file)).thenThrow(new InvalidFileException("Invalid file"));

        ResponseEntity<?> response = uploadController.uploadApplicants(file);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(Objects.requireNonNull(response.getBody()));
    }

    @Test
    void testDownloadApplicantsTemplate_CSV() {
        ResponseEntity<byte[]> response = uploadController.downloadApplicantsTemplate("csv");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("text/csv", Objects.requireNonNull(response.getHeaders().getContentType()).toString());

        assertTrue(new String(Objects.requireNonNull(response.getBody())).contains("firstName,middleName,lastName"));
        assertTrue(Objects.requireNonNull(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION)).get(0).contains("applicants_template.csv"));
    }

    @Test
    void testDownloadUsersTemplate_Excel() {
        ResponseEntity<byte[]> response = uploadController.downloadUsersTemplate("excel");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
                     Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        assertTrue(Objects.requireNonNull(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION)).get(0).contains("users_template.xlsx"));
    }

    @Test
    void testDownloadTemplate_InvalidFormat() {
        ResponseEntity<byte[]> response = uploadController.downloadParametersTemplate("pdf");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid format. Use 'csv' or 'excel'.", new String(Objects.requireNonNull(response.getBody())));
    }
}
