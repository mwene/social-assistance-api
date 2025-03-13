package com.social.assistance.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "villages")
public class Village {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne
    @JoinColumn(name = "sub_location_id", nullable = false)
    private SubLocation subLocation;
}
