package com.social.assistance.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ApplicationRequest {

    @NotNull(message = "Applicant ID is required")
    private Integer applicantId;

    @NotNull(message = "Programme ID is required")
    private Integer programmeId;
}
