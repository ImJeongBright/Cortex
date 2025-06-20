package org.example.ticket.reservation.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatUpdateRequest {

    private Integer seatFloor;
    private String seatSection;
    private Integer seatRow;
    private Integer seatNumber;

}
