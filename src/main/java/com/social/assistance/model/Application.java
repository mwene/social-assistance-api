package com.social.assistance.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;

    @ManyToOne
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate = LocalDate.now();

    @Column(nullable = false, length = 20)
    private String status = "Pending";

    @ManyToOne
    @JoinColumn(name = "maker_id")
    private User maker;

    @ManyToOne
    @JoinColumn(name = "checker_id")
    private User checker;

    @Column(nullable = false)
    private Boolean approved = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
