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
public class VenueHallFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "venuehall_floor")
    private Integer floor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venuehall_id")
    private VenueHall venueHall;

    @Builder.Default // 이 부분 학습 (OneToMany에서 Builder.Default를 사용했다.)
    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL)
    private List<VenueHallSection> sections = new ArrayList<>();
}
