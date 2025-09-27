package org.example.ticket;

import org.example.ticket.member.model.Member;
import org.example.ticket.member.repository.MemberRepository;
import org.example.ticket.performance.request.PerformanceDetailRequest;
import org.example.ticket.performance.request.PerformanceTimeRequest;
import org.example.ticket.performance.request.SeatPriceRequest;
import org.example.ticket.performance.response.PerformanceTimeResponse;
import org.example.ticket.performance.service.PerformanceService;
import org.example.ticket.performance.service.PerformanceTimeService;
import org.example.ticket.performance.service.SeatPriceService;
import org.example.ticket.reservation.request.ReservationRequest;
import org.example.ticket.reservation.model.Seat;
import org.example.ticket.reservation.repository.SeatRepository;
import org.example.ticket.reservation.service.ReservationFacade;
import org.example.ticket.reservation.service.ReservationService;
import org.example.ticket.reservation.service.SeatService;
import org.example.ticket.util.constant.SeatInfo;
import org.example.ticket.venue.dto.request.*;
import org.example.ticket.venue.model.Venue;
import org.example.ticket.venue.model.VenueHall;
import org.example.ticket.venue.service.VenueHallService;
import org.example.ticket.venue.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(ReservationConcurrencyTest.class);

    @Autowired private ReservationService reservationService;
    @Autowired private ReservationFacade reservationFacade;
    @Autowired private MemberRepository memberRepository;
    @Autowired private VenueService venueService;
    @Autowired private VenueHallService venueHallService;
    @Autowired private PerformanceService performanceService;
    @Autowired private SeatPriceService seatPriceService;
    @Autowired private PerformanceTimeService performanceTimeService;
    @Autowired private SeatService seatService;
    @Autowired private SeatRepository seatRepository;

    private Long targetSeatId;
    private Long performanceTimeId;
    private List<Member> members;
    private final int USER_COUNT = 5000; // 사용자 및 스레드 수를 상수로 관리

    @BeforeEach
    @Transactional
    void setUp() throws IOException {
        // === 1. 테스트 사용자 생성 ===
        members = new ArrayList<>();
        IntStream.range(0, USER_COUNT).forEach(i -> {
            Member member = Member.builder()
                    .walletAddress("0x" + i + "abcde")
                    .phoneNumber("010-0000-" + String.format("%04d", i))
                    .role("ROLE_USER")
                    .nickname("testuser" + i)
                    .smsVerified(true)
                    .walletVerified(true)
                    .build();
            members.add(member);
        });
        memberRepository.saveAll(members);

        // === 2. 공연장 및 좌석 데이터 준비 ===
        VenueRequest venueRequest = VenueRequest.builder().name("테스트 공연장").address("서울시 테스트구").build();
        VenueHallRequest hallRequest = VenueHallRequest.builder().name("테스트 홀").totalSeats(5).build();

        // ✨ [수정] Service가 저장된 Venue 객체를 반환한다고 가정
        Venue savedVenue = venueService.insertVenue(venueRequest, List.of(hallRequest));
        assertTrue(savedVenue.getVenueHalls() != null && !savedVenue.getVenueHalls().isEmpty(), "공연 홀이 생성되지 않았습니다.");
        VenueHall savedHall = savedVenue.getVenueHalls().get(0);
        Long hallId = savedHall.getId();

        VenueHallSeatRequest row1Seats = VenueHallSeatRequest.builder().seatInfo(SeatInfo.VIP).startSeatNumber(1).endSeatNumber(3).build();
        VenueHallRowRequest row1 = VenueHallRowRequest.builder().row(1).seats(List.of(row1Seats)).build();
        VenueHallSeatRequest row2Seats = VenueHallSeatRequest.builder().seatInfo(SeatInfo.S).startSeatNumber(1).endSeatNumber(2).build();
        VenueHallRowRequest row2 = VenueHallRowRequest.builder().row(2).seats(List.of(row2Seats)).build();
        VenueHallSectionRequest sectionA = VenueHallSectionRequest.builder().section("A").rows(List.of(row1, row2)).build();
        VenueHallFloorRequest floor1 = VenueHallFloorRequest.builder().floor(1).section(List.of(sectionA)).build();

        venueHallService.allocateEmptySeatTemplate(hallId, List.of(floor1));

        // === 3. 공연 및 가격 정책 데이터 준비 ===
        PerformanceDetailRequest performanceRequest = PerformanceDetailRequest.builder()
                .title("테스트 공연")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .build();

        // ✨ [수정] Service가 저장된 Performance ID를 반환한다고 가정
        Long performanceId = performanceService.registerPerformance(performanceRequest, null);

        List<SeatPriceRequest> priceRequests = List.of(
                SeatPriceRequest.builder().seatInfo(SeatInfo.VIP).price(150000).build(),
                SeatPriceRequest.builder().seatInfo(SeatInfo.S).price(120000).build()
        );
        seatPriceService.setSeatPrice(priceRequests, performanceId);

        // === 4. 공연 회차 및 좌석 재고 생성 ===
        List<PerformanceTimeRequest> timeRequests = List.of(
                PerformanceTimeRequest.builder()
                        .showDate(LocalDate.now().plusDays(5))
                        .showTime(LocalTime.of(19, 30))
                        .venueHallId(hallId)
                        .build()
        );

        // ✨ [수정] Service가 저장된 PerformanceTime 리스트를 반환한다고 가정
        List<PerformanceTimeResponse> savedTimes = performanceTimeService.allocatePerformanceTime(timeRequests, performanceId);
        assertTrue(!savedTimes.isEmpty(), "공연 회차가 생성되지 않았습니다.");
        this.performanceTimeId = savedTimes.get(0).getId();

        seatService.preprocessSeatDataWithNoAsync(this.performanceTimeId); // ✨ [수정] 동기 메서드 호출로 변경

        // === 5. 테스트 대상 좌석 ID 설정 ===
        List<Seat> seats = seatRepository.findAll();
        assertTrue(!seats.isEmpty(), "좌석 재고가 생성되지 않았습니다.");
        this.targetSeatId = seats.get(0).getId();
    }

    @Test
    @DisplayName("비관적 락을 사용하여 동일한 좌석에 100명의 다른 사용자가 예약 요청 시, 성능 측정 및 정합성 검증")
    void reserveSameSeatConcurrentlyWithDifferentUsers() throws InterruptedException {
        // given
        int threadCount = USER_COUNT;
        ExecutorService executorService = Executors.newFixedThreadPool(32); // CPU 코어 수에 맞춰 적절히 조절
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        ReservationRequest request = new ReservationRequest(this.performanceTimeId, List.of(this.targetSeatId));

        // when
        for (Member member : members) { // ✨ [수정] 변수 이름 변경 및 for-each 루프로 변경
            executorService.submit(() -> {
                long startTime = System.nanoTime();
                try {
                    reservationService.createReservation(member.getWalletAddress(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    long endTime = System.nanoTime();
                    responseTimes.add(endTime - startTime);
                    endLatch.countDown();
                }
            });
        }

        endLatch.await();
        executorService.shutdown();

        // then
        assertEquals(1, successCount.get(), "예약은 단 한 번만 성공해야 합니다.");
        assertEquals(threadCount - 1, failureCount.get(), "나머지 요청은 모두 실패해야 합니다.");
        logPerformance("비관적", threadCount, successCount, failureCount, responseTimes);
    }

    @Test
    @DisplayName("낙관적 락을 이용하여 동일한 좌석에 1000명의 다른 사용자가 예약 요청 시, 성능 측정 및 정합성 검증")
    void reserveSameSeatConcurrentlyWithOptimisticLock() throws InterruptedException {
        // given
        int threadCount = USER_COUNT;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        ReservationRequest request = new ReservationRequest(this.performanceTimeId, List.of(this.targetSeatId));

        // when
        for (Member member : members) { // ✨ [수정] 변수 이름 변경 및 for-each 루프로 변경
            executorService.submit(() -> {
                long startTime = System.nanoTime();
                try {
                    reservationService.createReservationWithOptimistic(member.getWalletAddress(), request);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) { // ✨ [수정] 구체적인 예외 처리
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("예상치 못한 예외 발생", e);
                    failureCount.incrementAndGet();
                } finally {
                    long endTime = System.nanoTime();
                    responseTimes.add(endTime - startTime);
                    endLatch.countDown();
                }
            });
        }

        endLatch.await();
        executorService.shutdown();

        // then
        assertEquals(1, successCount.get(), "예약은 단 한 번만 성공해야 합니다.");
        assertEquals(threadCount - 1, failureCount.get(), "나머지 요청은 모두 실패해야 합니다.");
        logPerformance("낙관적", threadCount, successCount, failureCount, responseTimes);
    }

    @Test
    @DisplayName("분산 락을 이용하여 동일한 좌석에 100명의 다른 사용자가 예약 요청 시, 성능 측정 및 정합성 검증")
    void reserveSameSeatConcurrentlyWithDistributionLock() throws InterruptedException {
        // given
        int threadCount = USER_COUNT; // ✨ [수정] 스레드 수 일치
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        ReservationRequest request = new ReservationRequest(this.performanceTimeId, List.of(this.targetSeatId));

        // when
        for (Member member : members) { // ✨ [수정] 변수 이름 변경 및 for-each 루프로 변경
            executorService.submit(() -> {
                long startTime = System.nanoTime();
                try {
                    reservationFacade.createReservationWithLock(member.getWalletAddress(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    long endTime = System.nanoTime();
                    responseTimes.add(endTime - startTime);
                    endLatch.countDown();
                }
            });
        }

        endLatch.await();
        executorService.shutdown();

        // then
        assertEquals(1, successCount.get(), "예약은 단 한 번만 성공해야 합니다.");
        assertEquals(threadCount - 1, failureCount.get(), "나머지 요청은 모두 실패해야 합니다.");
        logPerformance("분산", threadCount, successCount, failureCount, responseTimes);
    }

    // 로그 출력을 위한 헬퍼 메서드
    private void logPerformance(String type, int threadCount, AtomicInteger successCount, AtomicInteger failureCount, List<Long> responseTimes) {
        long minTimeMs = responseTimes.stream().min(Long::compareTo).orElse(0L) / 1_000_000;
        long maxTimeMs = responseTimes.stream().max(Long::compareTo).orElse(0L) / 1_000_000;
        double avgTimeMs = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;

        log.info("========== 동시성 테스트 결과 ({} 락) ==========", type);
        log.info("총 요청: {}건", threadCount);
        log.info("성공: {}건, 실패: {}건", successCount.get(), failureCount.get());
        log.info("최단 응답 시간: {}ms", minTimeMs);
        log.info("최장 응답 시간: {}ms", maxTimeMs);
        log.info("평균 응답 시간: {}ms", String.format("%.2f", avgTimeMs));
        log.info("==============================================");
    }
}