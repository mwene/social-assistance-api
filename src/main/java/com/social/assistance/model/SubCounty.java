package com.social.assistance.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "sub_counties")
public class SubCounty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne
    @JoinColumn(name = "county_id", nullable = false)
    private County county;
}
