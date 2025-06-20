package org.example.ticket.reservation.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticket.performance.model.Performance;
import org.example.ticket.performance.model.PerformanceTime;
import org.example.ticket.performance.model.SeatPrice;
import org.example.ticket.performance.repository.PerformanceTimeRepository;
import org.example.ticket.reservation.response.SeatResponse;
import org.example.ticket.reservation.model.Seat;
import org.example.ticket.reservation.repository.SeatRepository;
import org.example.ticket.util.constant.SeatInfo;
import org.example.ticket.venue.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final SeatRepository repository;
    private final PerformanceTimeRepository performanceTimeRepository;


    @Transactional
    public List<Seat> findAndLockSeatsByIds(List<Long> seatId) {
        return repository.findByIdsForUpdate(seatId);
    }

    @Transactional
    public void changeSeatsState(List<Seat> seats) {
        seats.forEach(Seat::markAsReserved);
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> viewEmptySeatList(Long performanceTimeId) {
        return repository.findByEmptySeat(performanceTimeId);
    }

    @Transactional
    public void preprocessSeatData(Long performanceTimeId) {

        List<Seat> unreservationSeat = new ArrayList<>();

        PerformanceTime performanceTime = performanceTimeRepository.findById(performanceTimeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 공연장(공연) 시간을 찾을 수 없습니다."));

        VenueHall venueHall = performanceTime.getVenueHall();
        Performance performance = performanceTime.getPerformance();

        Map<SeatInfo, Integer> priceMap = performance.getSeatPrices()
                .stream()
                .collect(
                        Collectors.toMap(
                                SeatPrice::getSeatInfo,
                                SeatPrice::getPrice
                        )
                );

        List<VenueHallFloor> floorList = venueHall.getFloorList();

        floorList.forEach(floorDTO -> {
            List<VenueHallSection> sections = floorDTO.getSections();
            sections.forEach(sectionDTO -> {
                List<VenueHallRow> rows = sectionDTO.getRows();
                rows.forEach(rowsDTO -> {
                    List<VenueHallSeat> seats = rowsDTO.getSeats();
                    Integer startSeatNumber = seats.getFirst().getStartSeatNumber();
                    Integer endSeatNumber = seats.getFirst().getEndSeatNumber();
                    SeatInfo seatInfo = seats.getFirst().getSeatInfo();
                    Integer price = priceMap.get(seatInfo);

                    for (int i = startSeatNumber; i <= endSeatNumber; i++) {
                        Seat seat = initializedSeat(floorDTO, sectionDTO, rowsDTO, price, performanceTime, i, seatInfo);
                        unreservationSeat.add(seat);
                    }

                });
            });
        });

        repository.saveAll(unreservationSeat);
    }

    private static Seat initializedSeat(VenueHallFloor floorDTO, VenueHallSection sectionDTO, VenueHallRow rowsDTO, Integer price, PerformanceTime performanceTime, int i, SeatInfo seatInfo) {
        return Seat.builder()
                .seatFloor(floorDTO.getFloor())
                .seatSection(sectionDTO.getSection())
                .seatRow(rowsDTO.getRow())
                .price(price)
                .performanceTime(performanceTime)
                .seatNumber(i)
                .seatType(seatInfo)
                .isReservation(false)
                .build();
    }


}
