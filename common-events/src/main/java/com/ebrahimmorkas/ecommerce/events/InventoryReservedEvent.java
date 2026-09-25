package com.ebrahimmorkas.ecommerce.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID orderNumber,
        String customerEmail,
        BigDecimal totalAmount,
        Instant occurredAt) {
}
