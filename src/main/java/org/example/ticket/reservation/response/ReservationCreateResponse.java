package org.example.ticket.reservation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.ticket.performance.model.Performance;
import org.example.ticket.reservation.model.Reservation;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReservationCreateResponse {

    private Long id;
    private Integer totalPrice;
    private String orderUid;

    private List<SeatResponse> responses;

    public static ReservationCreateResponse from(Reservation reservation) {

        return ReservationCreateResponse.builder()
                .id(reservation.getId())
                .totalPrice(reservation.getTotalPrice())
                .orderUid(reservation.getReservationCode())
                .responses(
                        reservation.getReservedSeats().stream().map(
                                        reservedSeat -> new SeatResponse(reservedSeat.getSeat())
                                )
                                .toList()
                )
                .build();

    }


}
