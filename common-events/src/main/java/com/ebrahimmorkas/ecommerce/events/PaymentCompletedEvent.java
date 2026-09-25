package com.ebrahimmorkas.ecommerce.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID orderNumber,
        UUID paymentId,
        BigDecimal amount,
        Instant occurredAt) {
}
