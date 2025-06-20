package org.example.ticket.performance.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Performer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artist_name")
    private String name;

}
