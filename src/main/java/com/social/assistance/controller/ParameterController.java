package com.social.assistance.controller;

import com.social.assistance.model.Parameter;
import com.social.assistance.repository.ParameterRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parameters")
@SecurityRequirement(name = "bearerAuth")
public class ParameterController {
    private final ParameterRepository parameterRepository;

    public ParameterController(ParameterRepository parameterRepository) {
        this.parameterRepository = parameterRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Parameter>> getParametersByCategory(@RequestParam String category, Pageable pageable) {
        return ResponseEntity.ok(parameterRepository.findByCategory(category, pageable));
    }
}
