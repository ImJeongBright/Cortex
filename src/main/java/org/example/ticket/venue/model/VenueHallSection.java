package org.example.ticket.venue.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VenueHallSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venuehall_section")
    private String section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venuehall_floor_id")
    private VenueHallFloor floor;

    @Builder.Default
    @OneToMany(mappedBy = "sections", cascade = CascadeType.ALL)
    private List<VenueHallRow> rows = new ArrayList<>();

}
