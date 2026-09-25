package com.ebrahimmorkas.ecommerce.inventory.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Records the outcome of a stock reservation per order. The unique order number makes event
 * handling idempotent: a redelivered {@code OrderCreatedEvent} finds the existing row and is skipped.
 */
@Entity
@Table(name = "stock_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, updatable = false)
    private UUID orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stock_reservation_items", joinColumns = @JoinColumn(name = "reservation_id"))
    private List<ReservationItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private StockReservation(UUID orderNumber, ReservationStatus status, List<ReservationItem> items) {
        this.orderNumber = orderNumber;
        this.status = status;
        this.items = new ArrayList<>(items);
    }

    public static StockReservation reserved(UUID orderNumber, List<ReservationItem> items) {
        return new StockReservation(orderNumber, ReservationStatus.RESERVED, items);
    }

    public static StockReservation rejected(UUID orderNumber) {
        return new StockReservation(orderNumber, ReservationStatus.REJECTED, List.of());
    }

    public void release() {
        status = ReservationStatus.RELEASED;
    }
}
