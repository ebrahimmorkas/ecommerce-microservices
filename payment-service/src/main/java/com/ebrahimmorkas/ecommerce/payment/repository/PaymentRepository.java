package com.ebrahimmorkas.ecommerce.payment.repository;

import com.ebrahimmorkas.ecommerce.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsByOrderNumber(UUID orderNumber);

    Optional<Payment> findByOrderNumber(UUID orderNumber);
}
