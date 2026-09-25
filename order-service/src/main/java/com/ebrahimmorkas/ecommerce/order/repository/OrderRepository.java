package com.ebrahimmorkas.ecommerce.order.repository;

import com.ebrahimmorkas.ecommerce.order.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByOrderNumber(UUID orderNumber);

    Page<Order> findByCustomerEmailIgnoreCase(String customerEmail, Pageable pageable);
}
