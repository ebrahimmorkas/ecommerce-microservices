package com.ebrahimmorkas.ecommerce.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderNumber,
        String customerEmail,
        List<Item> items,
        BigDecimal totalAmount,
        Instant occurredAt) {

    public record Item(String skuCode, int quantity) {
    }
}
