package org.example.ticket.reservation.service;


import com.siot.IamportRestClient.exception.IamportResponseException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticket.performance.model.Performance;
import org.example.ticket.member.model.Member;
import org.example.ticket.member.repository.MemberRepository;
import org.example.ticket.reservation.response.ReservationCreateResponse;
import org.example.ticket.reservation.model.Reservation;
import org.example.ticket.reservation.model.ReservedSeat;
import org.example.ticket.reservation.model.Seat;
import org.example.ticket.reservation.request.ReservationRequest;
import org.example.ticket.reservation.repository.ReservationRepository;
import org.example.ticket.reservation.response.ReservationSuccessResponse;
import org.example.ticket.util.constant.ReservationStatus;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final SeatService seatService;

    @Transactional
    public ReservationCreateResponse createReservation(String walletAddress, ReservationRequest request) {
        Member member = memberRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<Seat> seats = seatService.findAndLockSeatsByIds(request.getSeatIds());
        checkSeatsAvailability(seats);

        seatService.changeSeatsState(seats);

        int totalPrice = seats.stream().mapToInt(Seat::getPrice).sum();

        String reservationCode = member.makeReservationCode();

        Reservation reservation = Reservation.builder()
                .totalPrice(totalPrice)
                .member(member)
                .reservationCode(reservationCode)
                .reservationStatus(ReservationStatus.PENDING_PAYMENT)
                .build();

        List<ReservedSeat> reservedSeats = seats.stream()
                .map(seat -> ReservedSeat.builder().reservation(reservation).seat(seat).build())
                .toList();

        reservation.setReservedSeats(reservedSeats);

        reservationRepository.save(reservation);

        return ReservationCreateResponse.from(reservation);

    }

    @Transactional
    public ReservationCreateResponse createReservationWithDistribution(String walletAddress, ReservationRequest request) {
        Member member = memberRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<Seat> seats = seatService.findAndLockSeatsByIdsWithDistribution(request.getSeatIds());
        checkSeatsAvailability(seats);

        seatService.changeSeatsState(seats);

        int totalPrice = seats.stream().mapToInt(Seat::getPrice).sum();

        String reservationCode = member.makeReservationCode();

        Reservation reservation = Reservation.builder()
                .totalPrice(totalPrice)
                .member(member)
                .reservationCode(reservationCode)
                .reservationStatus(ReservationStatus.PENDING_PAYMENT)
                .build();

        List<ReservedSeat> reservedSeats = seats.stream()
                .map(seat -> ReservedSeat.builder().reservation(reservation).seat(seat).build())
                .toList();

        reservation.setReservedSeats(reservedSeats);

        reservationRepository.save(reservation);

        return ReservationCreateResponse.from(reservation);

    }

    @Transactional
    public ReservationCreateResponse createReservationWithOptimistic(String walletAddress, ReservationRequest request) {
        Member member = memberRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<Seat> seats = seatService.findAndLockSeatsByIdsWithOptimistic(request.getSeatIds());
        checkSeatsAvailability(seats);

        seatService.changeSeatsState(seats);

        int totalPrice = seats.stream().mapToInt(Seat::getPrice).sum();

        String reservationCode = member.makeReservationCode();

        Reservation reservation = Reservation.builder()
                .totalPrice(totalPrice)
                .member(member)
                .reservationCode(reservationCode)
                .reservationStatus(ReservationStatus.PENDING_PAYMENT)
                .build();

        List<ReservedSeat> reservedSeats = seats.stream()
                .map(seat -> ReservedSeat.builder().reservation(reservation).seat(seat).build())
                .toList();

        reservation.setReservedSeats(reservedSeats);

        reservationRepository.save(reservation);

        return ReservationCreateResponse.from(reservation);

    }


    /**
     *
     * @param reservationId
     * @return
     * @throws IamportResponseException
     * @throws IOException
     *
     *    1. reservationRepository.findById(reservationId): 예약 정보 조회
     *    2. reservation.getReservedSeats() -> map(ReservedSeat::getSeat): 예약된 좌석 정보 조회 (N+1 문제 발생 가능)
     *    3. reservationRepository.findByPerformance(reservationId): 공연 정보 조회
     *    4. reservationRepository.findByWalletAddressByOrganizer(reservationId): 주최사 지갑 주소 조회
     *    해당 부분에서 여러번의 쿼리로, 성능 저하가 될 수 있음, 해서 한 번의 쿼리로 모든 정보를 가져오도록 변경
     *
     */
    @Transactional
    public ReservationSuccessResponse confirmReservation(Long reservationId) throws IamportResponseException, IOException {


        Reservation reservation = reservationRepository.findByIdWithDetails(reservationId).
                orElseThrow(() -> new EntityNotFoundException("예약 정보를 확인 할 수 없습니다."));


        if(!reservation.getReservationStatus().equals(ReservationStatus.PENDING_PAYMENT)) {
            throw new EntityNotFoundException("결제 정보를 확인할 수 없거나 , 결제가 완료된 티켓입니다."); // custom Exception
        }

        List<Seat> seats = reservation.getReservedSeats().stream()
                .map(ReservedSeat::getSeat)// 2
                .toList();

        Performance performance = reservation.getReservedSeats().
                getFirst().getSeat().getPerformanceTime().getPerformance();

        reservation.changeReservationStatus(ReservationStatus.SUCCESS);

        seatService.changeSeatsState(seats);

        String byWalletAddressByOrganizer =
                reservation.getReservedSeats().getFirst().getSeat().getPerformanceTime().
                getPerformance().getOrganizer().getMember().getWalletAddress();

        return ReservationSuccessResponse.from(reservation, performance, byWalletAddressByOrganizer);
    }

    public void checkSeatsAvailability(List<Seat> seats) {

        boolean isReserved = seats.stream()
                .anyMatch(seat -> Boolean.TRUE.equals(seat.getIsReservation()));


        if (isReserved) throw new EntityExistsException("이미 예약 완료된 좌석입니다."); // customException

    }


}
