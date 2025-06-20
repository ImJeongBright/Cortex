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
public class VenueHallRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venuehall_section_row")
    private Integer row;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venuhall_zone_id")
    private VenueHallSection sections;

    @Builder.Default
    @OneToMany(mappedBy = "row", cascade = CascadeType.ALL)
    private List<VenueHallSeat> seats = new ArrayList<>();


}
