package com.social.assistance.service;

import com.social.assistance.dto.UserRegistrationRequest;
import com.social.assistance.exception.DuplicateResourceException;
import com.social.assistance.exception.InvalidStateException;
import com.social.assistance.exception.ResourceNotFoundException;
import com.social.assistance.model.User;
import com.social.assistance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest request;

    @BeforeEach
    void setUp() {
        request = new UserRegistrationRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setName("Test User");
        request.setRole("ROLE_VERIFIER");
    }

    @Test
    void registerUser_success() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.registerUser(request);

        assertEquals("testuser", user.getUsername());
        assertEquals("encodedPassword", user.getPassword());
        assertEquals("Test User", user.getName());
        assertEquals("ROLE_VERIFIER", user.getRole());
        assertTrue(user.getEnabled());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_duplicateUsername_throwsException() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_invalidRole_throwsException() {
        request.setRole("ROLE_INVALID");

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_success() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedOldPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.changePassword("testuser", "oldPassword", "newPassword");

        assertEquals("encodedNewPassword", user.getPassword());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void changePassword_userNotFound_throwsException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.changePassword("testuser", "oldPassword", "newPassword"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_invalidOldPassword_throwsException() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encodedOldPassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);

        assertThrows(InvalidStateException.class, () -> userService.changePassword("testuser", "wrongPassword", "newPassword"));
        verify(userRepository, never()).save(any(User.class));
    }
}
