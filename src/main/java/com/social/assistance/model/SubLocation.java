package com.social.assistance.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "sub_locations")
public class SubLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;
}
