package org.example.ticket;

import org.example.ticket.member.model.Member;
import org.example.ticket.member.repository.MemberRepository;
import org.example.ticket.performance.model.Performance;
import org.example.ticket.performance.model.PerformanceTime;
import org.example.ticket.performance.repository.PerformanceRepository;
import org.example.ticket.performance.repository.PerformanceTimeRepository;
import org.example.ticket.performance.request.PerformanceDetailRequest;
import org.example.ticket.performance.request.PerformanceTimeRequest;
import org.example.ticket.performance.request.SeatPriceRequest;
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
import org.example.ticket.venue.repository.VenueHallRepository;
import org.example.ticket.venue.repository.VenueRepository;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(ReservationConcurrencyTest.class);
    // --- 테스트 대상 서비스 ---
    @Autowired private ReservationService reservationService;

    // --- 데이터 설정을 위한 서비스 및 리포지토리 ---
    @Autowired private MemberRepository memberRepository;
    @Autowired private VenueService venueService;
    @Autowired private VenueHallService venueHallService;
    @Autowired private PerformanceService performanceService;
    @Autowired private SeatPriceService seatPriceService;
    @Autowired private PerformanceTimeService performanceTimeService;
    @Autowired private SeatService seatService;
    @Autowired private SeatRepository seatRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private PerformanceTimeRepository performanceTimeRepository;
    @Autowired private PerformanceRepository performanceRepository;
    @Autowired private VenueHallRepository venueHallRepository;
    @Autowired private ReservationFacade reservationFacade;

    private String memberId;
    private Long targetSeatId; // 모든 스레드가 동시에 요청할 좌석 ID
    private Long performanceTimeId;
    private List<Member> members;

    @BeforeEach
    @Transactional
    void setUp() throws IOException {

        int userCount = 100; // 테스트 스레드 수와 동일하게 설정
        this.members = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            Member member = Member.builder()
                    .walletAddress("0x" + i + "asduwqb22") // 고유한 지갑 주소
                    .phoneNumber("010-0000-" + String.format("%04d", i)) // 고유한 전화번호
                    .role("ROLE_USER")
                    .nickname("testuser" + i) // 고유한 닉네임
                    .smsVerified(true)
                    .walletVerified(true)
                    .build();
            members.add(member);
        }
        memberRepository.saveAll(members);

        // given: 테스트를 위한 데이터 준비 (Arrange)
        // 1. 공연장 및 홀 정보 DTO 생성
        VenueRequest venueRequest = VenueRequest.builder().name("테스트 공연장").address("서울시 테스트구").build();
        VenueHallRequest hallRequest = VenueHallRequest.builder().name("asddf").totalSeats(5).build(); // 테스트할 좌석 수와 일치시킴

        // 2. 공연장 및 기본 홀 정보 저장
        venueService.insertVenue(venueRequest, List.of(hallRequest));
        Venue savedVenue = venueRepository.findAll().getFirst();
        Long hallId = venueRepository.findByVenueHallsId(savedVenue.getId());

        // 3. (★★★★★ 여기가 빠졌던 부분 ★★★★★) 좌석 배치도(템플릿) DTO 생성
        VenueHallSeatRequest row1Seats = VenueHallSeatRequest.builder().seatInfo(SeatInfo.VIP).startSeatNumber(1).endSeatNumber(3).build(); // 1,2,3번 좌석
        VenueHallRowRequest row1 = VenueHallRowRequest.builder().row(1).seats(List.of(row1Seats)).build();

        VenueHallSeatRequest row2Seats = VenueHallSeatRequest.builder().seatInfo(SeatInfo.S).startSeatNumber(1).endSeatNumber(2).build(); // 1,2번 좌석
        VenueHallRowRequest row2 = VenueHallRowRequest.builder().row(2).seats(List.of(row2Seats)).build();

        VenueHallSectionRequest sectionA = VenueHallSectionRequest.builder().section("A").rows(List.of(row1, row2)).build();
        VenueHallFloorRequest floor1 = VenueHallFloorRequest.builder().floor(1).section(List.of(sectionA)).build();
        List<VenueHallFloorRequest> layoutRequest = List.of(floor1);

        // 4. 공연 정보 DTO 생성
        PerformanceDetailRequest performanceRequest = PerformanceDetailRequest.builder()
                .title("테스트 공연")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .build();

        // 5. 가격 정책 DTO 생성
        List<SeatPriceRequest> priceRequests = List.of(
                SeatPriceRequest.builder().seatInfo(SeatInfo.VIP).price(150000).build(),
                SeatPriceRequest.builder().seatInfo(SeatInfo.S).price(120000).build()
        );

        // when: 테스트하려는 로직 실행 (Act)
        // 6. 좌석 템플릿 등록
        venueHallService.allocateEmptySeatTemplate(hallId, layoutRequest);

        // 7. 공연 등록
        Long performanceId = performanceService.registerPerformance(performanceRequest, null);

        // 8. 가격 정책 등록
        seatPriceService.setSeatPrice(priceRequests, performanceId);

        // 9. 공연 회차 DTO 생성 및 등록
        List<PerformanceTimeRequest> timeRequests = List.of(
                PerformanceTimeRequest.builder()
                        .showDate(LocalDate.now().plusDays(5))
                        .showTime(LocalTime.of(19, 30))
                        .venueHallId(hallId)
                        .build()
        );
        performanceTimeService.allocatePerformanceTime(timeRequests, performanceId);
        performanceTimeId = performanceTimeRepository.findAll().getFirst().getId();

        // 10. 좌석 재고 생성 (핵심 테스트 대상)
        seatService.preprocessSeatDataWithNoAsync(performanceTimeId);

        // === 6. 테스트 대상 좌석 ID 설정 ===
        this.targetSeatId = seatRepository.findAll().getFirst().getId();
    }

    @Test
    @DisplayName("비관적 락을 사용하여 동일한 좌석에 동시에 1000명의 다른 사용자가 예약 요청 시, 성능 측정 및 정합성 검증")
    void reserveSameSeatConcurrentlyWithDifferentUsers() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1); // 시작 신호용
        CountDownLatch endLatch = new CountDownLatch(threadCount);   // 종료 대기용

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        ReservationRequest request = new ReservationRequest(this.performanceTimeId, List.of(this.targetSeatId));

        // when
        List<Callable<Void>> tasks = members.stream()
                .map(members -> (Callable<Void>) () -> {
                    startLatch.await(); // 모든 스레드가 여기서 대기
                    long startTime = System.nanoTime();
                    try {
                        reservationService.createReservation(members.getWalletAddress(), request);

                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        long endTime = System.nanoTime();
                        responseTimes.add(endTime - startTime);
                        endLatch.countDown();
                    }
                    return null; // Callable<Void>는 null을 반환해야 한다.
                })
                .toList();

        tasks.forEach(executorService::submit);
        startLatch.countDown(); // 신호탄! 모든 스레드 동시 시작
        endLatch.await(); // 모든 스레드가 끝날 때까지 대기
        executorService.shutdown();

        // then
        // 1. 정합성 검증
        assertEquals(1, successCount.get(), "예약은 단 한 번만 성공해야 합니다.");
        assertEquals(threadCount - 1, failureCount.get(), "나머지 요청은 모두 실패해야 합니다.");

        // 2. 성능 지표 계산 및 출력
        long minTimeMs = responseTimes.stream().min(Long::compareTo).orElse(0L) / 1_000_000;
        long maxTimeMs = responseTimes.stream().max(Long::compareTo).orElse(0L) / 1_000_000;
        double avgTimeMs = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;

        testLog(threadCount, successCount, failureCount, minTimeMs, maxTimeMs, avgTimeMs, "비관적");
    }



    @Test
    @DisplayName("낙관적 락을 이용하여 동일한 좌석에 동시에 1000명의 다른 사용자가 예약 요청 시, 성능 측정 및 정합성 검증")
    void reserveSameSeatConcurrentlyWithOptimisticLock() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1); // 시작 신호용
        CountDownLatch endLatch = new CountDownLatch(threadCount);   // 종료 대기용

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        ReservationRequest request = new ReservationRequest(this.performanceTimeId, List.of(this.targetSeatId));

        // when
        List<Callable<Void>> tasks = members.stream()
                .map(members -> (Callable<Void>) () -> {
                    startLatch.await(); // 모든 스레드가 여기서 대기
                    long startTime = System.nanoTime();
                    try {
                        reservationService.createReservationWithOptimistic(members.getWalletAddress(), request);

                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        long endTime = System.nanoTime();
                        responseTimes.add(endTime - startTime);
                        endLatch.countDown();
                    }
                    return null; // Callable<Void>는 null을 반환해야 한다.
                })
                .toList();

        tasks.forEach(executorService::submit);
        startLatch.countDown(); // 신호탄! 모든 스레드 동시 시작
        endLatch.await(); // 모든 스레드가 끝날 때까지 대기
        executorService.shutdown();

        // then
        // 1. 정합성 검증
        assertEquals(1, successCount.get(), "예약은 단 한 번만 성공해야 합니다.");
        assertEquals(threadCount - 1, failureCount.get(), "나머지 요청은 모두 실패해야 합니다.");

        // 2. 성능 지표 계산 및 출력
        long minTimeMs = responseTimes.stream().min(Long::compareTo).orElse(0L) / 1_000_000;
        long maxTimeMs = responseTimes.stream().max(Long::compareTo).orElse(0L) / 1_000_000;
        double avgTimeMs = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;

        testLog(threadCount, successCount, failureCount, minTimeMs, maxTimeMs, avgTimeMs, "낙관적");
    }

    private static void testLog(int threadCount, AtomicInteger successCount, AtomicInteger failureCount, long minTimeMs, long maxTimeMs, double avgTimeMs, String type) {
        log.info("========== 동시성 테스트 결과 ({} 락) ==========", type);
        log.info("총 요청: {}건", threadCount);
        log.info("성공: {}건, 실패: {}건", successCount.get(), failureCount.get());
        log.info("최단 응답 시간: {}ms", minTimeMs);
        log.info("최장 응답 시간: {}ms", maxTimeMs);
        log.info("평균 응답 시간: {}ms", String.format("%.2f", avgTimeMs));
        log.info("==============================================");
    }

    @Test
    @DisplayName("분산락을 이용하여 동일한 좌석에 동시에 1000명의 다른 사용자가 예약 요청 시, 성능 측정 및 정합성 검증")
    void reserveSameSeatConcurrentlyWithDistributionLock() throws InterruptedException {
        // given
        int threadCount = 3000;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1); // 시작 신호용
        CountDownLatch endLatch = new CountDownLatch(threadCount);   // 종료 대기용

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        ReservationRequest request = new ReservationRequest(this.performanceTimeId, List.of(this.targetSeatId));

        // when
        List<Callable<Void>> tasks = members.stream()
                .map(members -> (Callable<Void>) () -> {
                    startLatch.await(); // 모든 스레드가 여기서 대기
                    long startTime = System.nanoTime();
                    try {
                        reservationFacade.createReservationWithLock(members.getWalletAddress(), request);

                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        long endTime = System.nanoTime();
                        responseTimes.add(endTime - startTime);
                        endLatch.countDown();
                    }
                    return null; // Callable<Void>는 null을 반환해야 한다.
                })
                .toList();

        tasks.forEach(executorService::submit);
        startLatch.countDown(); // 신호탄! 모든 스레드 동시 시작
        endLatch.await(); // 모든 스레드가 끝날 때까지 대기
        executorService.shutdown();

        // then
        // 1. 정합성 검증
        assertEquals(1, successCount.get(), "예약은 단 한 번만 성공해야 합니다.");
        assertEquals(threadCount - 1, failureCount.get(), "나머지 요청은 모두 실패해야 합니다.");

        // 2. 성능 지표 계산 및 출력
        long minTimeMs = responseTimes.stream().min(Long::compareTo).orElse(0L) / 1_000_000;
        long maxTimeMs = responseTimes.stream().max(Long::compareTo).orElse(0L) / 1_000_000;
        double avgTimeMs = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;

        testLog(threadCount, successCount, failureCount, minTimeMs, maxTimeMs, avgTimeMs, "분산");
    }

}