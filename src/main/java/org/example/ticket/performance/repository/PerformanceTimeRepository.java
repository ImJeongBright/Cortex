package org.example.ticket.performance.repository;

import org.example.ticket.performance.model.PerformanceTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceTimeRepository extends JpaRepository<PerformanceTime, Long> {
}
