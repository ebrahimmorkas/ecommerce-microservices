package com.ebrahimmorkas.ecommerce.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One payment attempt per order; the unique order number makes charge handling idempotent. */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true, updatable = false)
    private UUID orderNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private Payment(UUID orderNumber, BigDecimal amount, PaymentStatus status, String providerReference,
                    String failureReason) {
        this.orderNumber = orderNumber;
        this.amount = amount;
        this.status = status;
        this.providerReference = providerReference;
        this.failureReason = failureReason;
    }

    public static Payment completed(UUID orderNumber, BigDecimal amount, String providerReference) {
        return new Payment(orderNumber, amount, PaymentStatus.COMPLETED, providerReference, null);
    }

    public static Payment failed(UUID orderNumber, BigDecimal amount, String reason) {
        return new Payment(orderNumber, amount, PaymentStatus.FAILED, null, reason);
    }
}
