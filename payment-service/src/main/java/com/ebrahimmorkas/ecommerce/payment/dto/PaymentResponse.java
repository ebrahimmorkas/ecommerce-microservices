package com.ebrahimmorkas.ecommerce.payment.dto;

import com.ebrahimmorkas.ecommerce.payment.domain.Payment;
import com.ebrahimmorkas.ecommerce.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID orderNumber,
        BigDecimal amount,
        PaymentStatus status,
        String failureReason,
        Instant createdAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderNumber(), payment.getAmount(),
                payment.getStatus(), payment.getFailureReason(), payment.getCreatedAt());
    }
}
