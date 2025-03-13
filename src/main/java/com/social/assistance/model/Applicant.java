package com.social.assistance.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "applicants")
public class Applicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @ManyToOne
    @JoinColumn(name = "sex_id", nullable = false)
    private Parameter sex;

    @Column(nullable = false)
    private Integer age;

    @ManyToOne
    @JoinColumn(name = "marital_status_id", nullable = false)
    private Parameter maritalStatus;

    @Column(name = "id_number", nullable = false, length = 20, unique = true)
    private String idNumber;

    @ManyToOne
    @JoinColumn(name = "village_id", nullable = false)
    private Village village;

    @Column(name = "postal_address", length = 255)
    private String postalAddress;

    @Column(name = "physical_address", length = 255)
    private String physicalAddress;

    @Column(length = 20)
    private String telephone;

    @Column(name = "verification_status", nullable = false, length = 20)
    private String verificationStatus = "Pending";

    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
