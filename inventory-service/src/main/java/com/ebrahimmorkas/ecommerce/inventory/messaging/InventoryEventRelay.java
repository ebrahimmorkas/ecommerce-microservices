package com.ebrahimmorkas.ecommerce.inventory.messaging;

import com.ebrahimmorkas.ecommerce.events.InventoryReservationFailedEvent;
import com.ebrahimmorkas.ecommerce.events.InventoryReservedEvent;
import com.ebrahimmorkas.ecommerce.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Forwards domain events to Kafka only after the database transaction commits, so consumers never
 * see an event for a change that was rolled back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class InventoryEventRelay {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener
    void on(InventoryReservedEvent event) {
        send(Topics.INVENTORY_RESERVED, event.orderNumber(), event);
    }

    @TransactionalEventListener
    void on(InventoryReservationFailedEvent event) {
        send(Topics.INVENTORY_RESERVATION_FAILED, event.orderNumber(), event);
    }

    private void send(String topic, UUID orderNumber, Object event) {
        kafkaTemplate.send(topic, orderNumber.toString(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} for order {}", event.getClass().getSimpleName(), orderNumber, ex);
            }
        });
    }
}
