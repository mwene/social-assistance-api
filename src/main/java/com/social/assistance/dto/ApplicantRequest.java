package com.social.assistance.dto;

import lombok.Data;

import javax.validation.constraints.*;

@Data
public class ApplicantRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Middle name must not exceed 50 characters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotNull(message = "Sex ID is required")
    private Integer sexId;

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be non-negative")
    private Integer age;

    @NotNull(message = "Marital status ID is required")
    private Integer maritalStatusId;

    @NotBlank(message = "ID number is required")
    @Size(max = 20, message = "ID number must not exceed 20 characters")
    private String idNumber;

    @NotNull(message = "Village ID is required")
    private Integer villageId;

    @Size(max = 255, message = "Postal address must not exceed 255 characters")
    private String postalAddress;

    @Size(max = 255, message = "Physical address must not exceed 255 characters")
    private String physicalAddress;

    @Size(max = 20, message = "Telephone must not exceed 20 characters")
    private String telephone;

    private Integer programmeId;
}
