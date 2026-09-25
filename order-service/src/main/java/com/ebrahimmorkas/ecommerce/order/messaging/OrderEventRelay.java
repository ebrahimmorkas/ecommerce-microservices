package com.ebrahimmorkas.ecommerce.order.messaging;

import com.ebrahimmorkas.ecommerce.events.OrderCreatedEvent;
import com.ebrahimmorkas.ecommerce.events.OrderStatusChangedEvent;
import com.ebrahimmorkas.ecommerce.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Forwards domain events to Kafka only after the database transaction commits, so consumers never
 * see an event for an order that was rolled back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class OrderEventRelay {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener
    void on(OrderCreatedEvent event) {
        send(Topics.ORDER_CREATED, event.orderNumber(), event);
    }

    @TransactionalEventListener
    void on(OrderStatusChangedEvent event) {
        send(Topics.ORDER_STATUS_CHANGED, event.orderNumber(), event);
    }

    private void send(String topic, UUID orderNumber, Object event) {
        kafkaTemplate.send(topic, orderNumber.toString(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} for order {}", event.getClass().getSimpleName(), orderNumber, ex);
            }
        });
    }
}
