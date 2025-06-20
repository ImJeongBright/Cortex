package org.example.ticket.payment.repository;

import org.example.ticket.payment.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
}
