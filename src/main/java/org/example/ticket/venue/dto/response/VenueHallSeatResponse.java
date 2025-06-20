package org.example.ticket.venue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.ticket.util.constant.SeatInfo;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VenueHallSeatResponse {

    private SeatInfo seatInfo;
    private Integer startSeatNumber;
    private Integer endSeatNumber;

}
