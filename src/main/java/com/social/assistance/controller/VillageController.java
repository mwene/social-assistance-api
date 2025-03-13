package com.social.assistance.controller;

import com.social.assistance.model.Village;
import com.social.assistance.repository.VillageRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/villages")
@SecurityRequirement(name = "bearerAuth")
public class VillageController {
    private final VillageRepository villageRepository;

    public VillageController(VillageRepository villageRepository) {
        this.villageRepository = villageRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Village>> getVillages(Pageable pageable) {
        return ResponseEntity.ok(villageRepository.findAll(pageable));
    }
}
