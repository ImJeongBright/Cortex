package org.example.ticket.performance.repository;

import org.example.ticket.performance.model.SeatPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatPriceRepository extends JpaRepository<SeatPrice, Long> {
}
