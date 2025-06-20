package org.example.ticket.performance.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.ticket.performance.request.SeatPriceRequest;
import org.example.ticket.performance.response.SeatPriceResponse;
import org.example.ticket.performance.model.Performance;
import org.example.ticket.performance.model.SeatPrice;
import org.example.ticket.performance.repository.PerformanceRepository;
import org.example.ticket.performance.repository.SeatPriceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatPriceService {

    private final PerformanceRepository performanceRepository;
    private final SeatPriceRepository seatPriceRepository;


    public List<SeatPriceResponse> setSeatPrice(List<SeatPriceRequest> seatPriceRequestList,
                                                 Long performanceId) {

        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new EntityNotFoundException("공연 정보를 찾을 수 없습니다."));

        List<SeatPrice> seatPrices = seatPriceRequestList
                .stream()
                .map(
                        dto -> {
                            return SeatPrice.builder()
                                    .performance(performance)
                                    .price(dto.getPrice())
                                    .seatInfo(dto.getSeatInfo())
                                    .build();
                        }
                ).toList();

        seatPriceRepository.saveAll(seatPrices);

        return SeatPriceResponse.from(seatPrices);
    }


}
