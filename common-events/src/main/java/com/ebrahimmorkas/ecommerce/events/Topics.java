package com.ebrahimmorkas.ecommerce.events;

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

    private Topics() {
    }
}
