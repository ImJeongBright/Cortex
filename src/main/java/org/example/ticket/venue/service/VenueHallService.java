package org.example.ticket.venue.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.ticket.venue.dto.request.*;
import org.example.ticket.venue.model.*;
import org.example.ticket.venue.repository.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

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

    @Async
    @Transactional
    public void allocateEmptySeatTemplate(Long hallId, List<VenueHallFloorRequest> floorRequestList) {

        VenueHall hall = venueHallRepository.findById(hallId)
                .orElseThrow(() -> new EntityNotFoundException("공연장을 찾을 수 없습니다."));

        processFloor(floorRequestList, hall);


    }

    private static void processFloor(List<VenueHallFloorRequest> floorRequestList, VenueHall hall) {

        floorRequestList.forEach(floorDTO -> {
            VenueHallFloor floors = VenueHallFloor.builder()
                    .floor(floorDTO.getFloor())
                    .venueHall(hall)
                    .build();
            hall.getFloorList().add(floors);
            processSection(floors, floorDTO.getSection());
        });

    }

    private static void processSection(VenueHallFloor floors , List<VenueHallSectionRequest> sectionList) {
        sectionList.forEach(sectionDTO -> {
            VenueHallSection sections = VenueHallSection.builder()
                    .floor(floors)
                    .section(sectionDTO.getSection())
                    .build();
            floors.getSections().add(sections);
            processRow(sections, sectionDTO.getRows());
        });
    }

    private static void processRow(VenueHallSection sections , List<VenueHallRowRequest> rowList) {
        rowList.forEach(rowDTO -> {
            VenueHallRow rows = VenueHallRow.builder()
                    .row(rowDTO.getRow())
                    .sections(sections)
                    .build();
            sections.getRows().add(rows);
            processSeats(rows, rowDTO.getSeats());
        });
    }

    private static void processSeats(VenueHallRow rows, List<VenueHallSeatRequest> seatList) {
        seatList.forEach(seatDTO -> {
            Integer startNum = seatDTO.getStartSeatNumber();
            Integer endNum = seatDTO.getEndSeatNumber();

            IntStream.rangeClosed(startNum, endNum).mapToObj(seatNumber ->
                VenueHallSeat.builder()
                        .seatInfo(seatDTO.getSeatInfo())
                        .startSeatNumber(startNum)
                        .endSeatNumber(endNum)
                        .seatNumber(seatNumber)
                        .row(rows)
                        .build())
                    .forEach(seat -> rows.getSeats().add(seat));
        });
    }

}
