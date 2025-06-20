package org.example.ticket.venue.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VenueHallResponse {

    private Long hallId;
    private String name;

}
