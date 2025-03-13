package com.social.assistance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.assistance.dto.UserRegistrationRequest;
import com.social.assistance.model.User;
import com.social.assistance.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void registerUser_success() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("newuser");
        request.setPassword("pass123");
        request.setName("New User");
        request.setRole("ROLE_VERIFIER");

        User user = new User();
        user.setUsername("newuser");
        user.setName("New User");
        user.setRole("ROLE_VERIFIER");

        when(userService.registerUser(any(UserRegistrationRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.role").value("ROLE_VERIFIER"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void registerUser_accessDenied() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setUsername("newuser");
        request.setPassword("pass123");
        request.setName("New User");
        request.setRole("ROLE_VERIFIER");

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_success() throws Exception {
        User user = new User();
        user.setUsername("admin");
        user.setName("Admin User");
        user.setRole("ROLE_ADMIN");

        when(userService.getAllUsers()).thenReturn(Collections.singletonList(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].name").value("Admin User"))
                .andExpect(jsonPath("$[0].role").value("ROLE_ADMIN"));
    }
}
