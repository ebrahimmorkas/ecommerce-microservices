package com.ebrahimmorkas.ecommerce.inventory.repository;

import com.ebrahimmorkas.ecommerce.inventory.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    boolean existsByOrderNumber(UUID orderNumber);

    Optional<StockReservation> findByOrderNumber(UUID orderNumber);
}
