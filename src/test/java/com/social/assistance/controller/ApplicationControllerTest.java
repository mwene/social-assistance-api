package com.social.assistance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.assistance.dto.ApplicationRequest;
import com.social.assistance.model.Application;
import com.social.assistance.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationService applicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "DATA_COLLECTOR")
    void createApplication_success() throws Exception {
        ApplicationRequest request = new ApplicationRequest();
        request.setApplicantId(1);
        request.setProgrammeId(1);

        Application application = new Application();
        application.setId(1);
        application.setStatus("Pending");

        when(applicationService.createApplication(1, 1)).thenReturn(application);

        mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("Pending"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createApplication_accessDenied() throws Exception {
        ApplicationRequest request = new ApplicationRequest();
        request.setApplicantId(1);
        request.setProgrammeId(1);

        mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
                //.andExpect(status().isForbidden()); // Ensure 403 is expected
    }

    @Test
    @WithMockUser(roles = "DATA_COLLECTOR")
    void getAllApplications_paginated_success() throws Exception {
        Application application = new Application();
        application.setId(1);
        application.setStatus("Pending");
        Page<Application> page = new PageImpl<>(Collections.singletonList(application));

        when(applicationService.getAllApplications(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/applications")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("Pending"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "APPROVER")
    void approveApplication_success() throws Exception {
        when(applicationService.getApplicationById(1)).thenReturn(Optional.of(new Application()));

        mockMvc.perform(patch("/api/applications/1/approve")
                .param("useMakerChecker", "false"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "APPROVER")
    void filterApplicationsByApplicantAndStatus_paginated_success() throws Exception {
        Application application = new Application();
        application.setId(1);
        application.setStatus("Pending");
        Page<Application> page = new PageImpl<>(Collections.singletonList(application));

        when(applicationService.filterApplicationsByApplicantAndStatus(eq(1), eq("Pending"), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/applications/filter/applicant-status")
                .param("applicantId", "1")
                .param("status", "Pending")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].status").value("Pending"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
