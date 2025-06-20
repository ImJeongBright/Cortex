package org.example.ticket.reservation.request;

import lombok.*;
import org.example.ticket.util.constant.SeatInfo;
import org.example.ticket.reservation.model.Seat;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatRequest {

    private Long seatId;
    private Integer seatFloor;
    private String seatSection;
    private Integer seatRow;
    private Integer seatNumber;
    private SeatInfo seatType;
    private Integer price;
    private Boolean isReservation;


    public static SeatRequest from(Seat seat) {
        return SeatRequest.builder()
                .seatId(seat.getId())
                .seatFloor(seat.getSeatFloor())
                .seatSection(seat.getSeatSection())
                .seatRow(seat.getSeatRow())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .price(seat.getPrice())
                .isReservation(seat.getIsReservation())
                .build();
    }

}
