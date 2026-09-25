package com.ebrahimmorkas.ecommerce.events;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservationFailedEvent(
        UUID orderNumber,
        String reason,
        Instant occurredAt) {
}
