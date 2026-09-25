package com.ebrahimmorkas.ecommerce.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID orderNumber,
        String reason,
        Instant occurredAt) {
}
