package org.example.ticket.performance.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.ticket.performance.request.PerformanceTimeRequest;
import org.example.ticket.performance.response.PerformanceTimeResponse;
import org.example.ticket.performance.model.Performance;
import org.example.ticket.performance.model.PerformanceTime;
import org.example.ticket.performance.repository.PerformanceRepository;
import org.example.ticket.performance.repository.PerformanceTimeRepository;
import org.example.ticket.venue.model.VenueHall;
import org.example.ticket.venue.repository.VenueHallRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceTimeService {


    private final PerformanceTimeRepository performanceTimeRepository;
    private final PerformanceRepository performanceRepository;
    private final VenueHallRepository venueHallRepository;

    public List<PerformanceTimeResponse> allocatePerformanceTime(List<PerformanceTimeRequest> performanceTimeRequests,
                                                                 Long performanceId) {


        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new EntityNotFoundException("공연 정보를 찾을 수 없습니다."));



        List<PerformanceTime> performanceTimes = performanceTimeRequests
                .stream()
                .map(
                        dto -> {

                            VenueHall venueHall = venueHallRepository.findById(dto.getVenueHallId())
                                    .orElseThrow(() -> new EntityNotFoundException("공연장 정보를 찾을 수 없습니다."));

                            return PerformanceTime.builder()
                                    .performance(performance)
                                    .venueHall(venueHall)
                                    .showDate(dto.getShowDate())
                                    .showTime(dto.getShowTime())
                                    .build();
                        }
                ).toList();

        performanceTimeRepository.saveAll(performanceTimes);

        return PerformanceTimeResponse.from(performanceTimes);
    }

}
