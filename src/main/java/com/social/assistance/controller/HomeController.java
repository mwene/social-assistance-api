package com.social.assistance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HomeController {
    @GetMapping
    @Operation(summary = "Home", description = "Social Assistance API Home Page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Applicants retrieved successfully")
    })
    public ResponseEntity<String> showHome() {
        return ResponseEntity.ok("Social Assistance API, @2025");
    }
}
