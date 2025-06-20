package org.example.ticket.venue.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.ticket.util.constant.SeatInfo;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VenueHallSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "venuehall_seat_information")
    private SeatInfo seatInfo;

    @Column(name = "venuhall_start_seatnumber_in_row")
    private Integer startSeatNumber;

    @Column(name = "venuhall_end_seatnumber_in_row")
    private Integer endSeatNumber;

    @Column(name = "venuehall_seatnumber")
    private Integer seatNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venuhall_row_id")
    private VenueHallRow row;

}
