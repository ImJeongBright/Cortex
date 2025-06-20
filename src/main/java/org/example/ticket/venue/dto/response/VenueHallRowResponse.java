package org.example.ticket.venue.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VenueHallRowResponse {

    private Integer row;
    private List<VenueHallSeatResponse> seats;

}
