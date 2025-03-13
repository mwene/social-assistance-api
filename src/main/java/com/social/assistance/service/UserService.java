package com.social.assistance.service;

import com.social.assistance.dto.UserRegistrationRequest;
import com.social.assistance.exception.DuplicateResourceException;
import com.social.assistance.exception.InvalidStateException;
import com.social.assistance.exception.ResourceNotFoundException;
import com.social.assistance.model.User;
import com.social.assistance.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager; // For stored procedures (optional)

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Valid roles for the application
    private static final List<String> VALID_ROLES = Arrays.asList(
            "ROLE_ADMIN", "ROLE_DATA_COLLECTOR", "ROLE_VERIFIER", "ROLE_APPROVER", "ROLE_USER"
    );

    /**
     * Register a new user (restricted to ROLE_ADMIN).
     * @param request User registration details
     * @return Created User entity
     * @throws DuplicateResourceException if username already exists
     * @throws IllegalArgumentException if role is invalid
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public User registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (!VALID_ROLES.contains(request.getRole())) {
            throw new IllegalArgumentException("Invalid role: " + request.getRole() + ". Valid roles are: " + VALID_ROLES);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(request.getRole());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setEnabled(true);

        // Optional: Use stored procedure instead
        /*
        entityManager.createNativeQuery("CALL register_user(:username, :password, :name, :role)")
                .setParameter("username", request.getUsername())
                .setParameter("password", passwordEncoder.encode(request.getPassword()))
                .setParameter("name", request.getName())
                .setParameter("role", request.getRole())
                .executeUpdate();
        return userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found after registration"));
        */

        return userRepository.save(user);
    }

    /**
     * Change password for the authenticated user.
     * @param username Current user's username
     * @param oldPassword Current password
     * @param newPassword New password
     * @throws ResourceNotFoundException if user not found
     * @throws InvalidStateException if old password is incorrect
     */
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidStateException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));

        // Optional: Use stored procedure instead
        /*
        entityManager.createNativeQuery("CALL change_user_password(:userId, :newPassword)")
                .setParameter("userId", user.getId())
                .setParameter("newPassword", passwordEncoder.encode(newPassword))
                .executeUpdate();
        */

        userRepository.save(user);
    }

    /**
     * Get all users (for admin use).
     * @return List of all users
     */
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get user by username (for internal use, e.g., authentication).
     * @param username Username to look up
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    /**
     * Get user ID by username (for maker-checker or other services).
     * @param username Username to look up
     * @return User ID
     * @throws ResourceNotFoundException if user not found
     */
    public Integer getUserIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}
