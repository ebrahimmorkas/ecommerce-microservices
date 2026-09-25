package com.ebrahimmorkas.ecommerce.events;

import java.util.List;

/**
 * Kafka topic names. Every message is keyed by order number so all events for one order land on
 * the same partition and are consumed in order.
 */
public final class Topics {

    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_STATUS_CHANGED = "order.status-changed";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_RESERVATION_FAILED = "inventory.reservation-failed";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";

    /**
     * Every saga topic. Each service provisions all of them at startup so a consumer can never cause
     * the broker to auto-create a topic with a single partition before its producer declares it.
     */
    public static final List<String> ALL = List.of(ORDER_CREATED, ORDER_STATUS_CHANGED, INVENTORY_RESERVED,
            INVENTORY_RESERVATION_FAILED, PAYMENT_COMPLETED, PAYMENT_FAILED);

    public static final int PARTITIONS = 3;

    private Topics() {
    }
}
