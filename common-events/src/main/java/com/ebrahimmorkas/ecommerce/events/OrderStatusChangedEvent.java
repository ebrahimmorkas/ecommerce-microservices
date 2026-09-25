package com.ebrahimmorkas.ecommerce.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderNumber,
        String customerEmail,
        String status,
        BigDecimal totalAmount,
        String reason,
        Instant occurredAt) {
}
