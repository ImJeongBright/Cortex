package org.example.ticket.venue.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.ticket.venue.dto.request.VenueHallFloorRequest;
import org.example.ticket.venue.dto.request.VenueHallRequest;
import org.example.ticket.venue.model.*;
import org.example.ticket.venue.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueHallService {

    private final VenueHallRepository venueHallRepository;

    public void registerVenueHallInformation(Venue venue, List<VenueHallRequest> venueHallRequest) {

        List<VenueHall> venueHallList = venueHallRequest.stream()
                .map(vq -> {
                    return VenueHall.builder()
                            .totalSeats(vq.getTotalSeats())
                            .name(vq.getName())
                            .venue(venue)
                            .build();
                })
                .toList();


        venueHallRepository.saveAll(venueHallList);
    }


    @Transactional
    public void allocateEmptySeatTemplate(Long hallId, List<VenueHallFloorRequest> floorRequestList) {

        VenueHall hall = venueHallRepository.findById(hallId)
                .orElseThrow(() -> new EntityNotFoundException("공연장을 찾을 수 없습니다."));

        floorRequestList.forEach(floorDTO -> {

            VenueHallFloor floors = VenueHallFloor.builder()
                    .floor(floorDTO.getFloor())
                    .venueHall(hall)
                    .build();
            hall.getFloorList().add(floors);

            floorDTO.getSection().forEach(sectionDTO -> {
                VenueHallSection sections = VenueHallSection.builder()
                        .floor(floors)
                        .section(sectionDTO.getSection())
                        .build();
                floors.getSections().add(sections);

                sectionDTO.getRows().forEach(rowDTO -> {
                    VenueHallRow rows = VenueHallRow.builder()
                            .row(rowDTO.getRow())
                            .sections(sections)
                            .build();
                    sections.getRows().add(rows);

                    rowDTO.getSeats().forEach(seatDTO -> {
                        Integer startNum = seatDTO.getStartSeatNumber();
                        Integer endNum = seatDTO.getEndSeatNumber();

                        for(int i = startNum; i <= endNum + 1; i++) {
                            VenueHallSeat seat = VenueHallSeat.builder()
                                    .seatInfo(seatDTO.getSeatInfo())
                                    .startSeatNumber(startNum)
                                    .endSeatNumber(endNum)
                                    .seatNumber(i)
                                    .row(rows)
                                    .build();
                            rows.getSeats().add(seat);

                        }
                    });
                });

            });
        });

    }
}
