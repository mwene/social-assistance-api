package com.social.assistance.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "counties")
public class County {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;
}
