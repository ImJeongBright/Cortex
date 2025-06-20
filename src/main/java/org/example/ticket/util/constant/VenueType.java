package org.example.ticket.util.constant;

import lombok.Getter;

@Getter
public enum VenueType {

    MUSICAL("뮤지컬"),
    SPORT("스포츠"),
    CONCERT("콘서트");


    private final String venueType;

    VenueType(String venueType) {
        this.venueType = venueType;
    }
}
