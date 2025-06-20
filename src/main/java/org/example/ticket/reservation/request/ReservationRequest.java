package org.example.ticket.reservation.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ReservationRequest {

    @NotEmpty
    @JsonProperty("seatIds")
    private List<Long> seatIds;

}
