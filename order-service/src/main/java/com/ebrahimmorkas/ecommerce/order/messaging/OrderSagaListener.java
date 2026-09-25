package com.ebrahimmorkas.ecommerce.order.messaging;

import com.ebrahimmorkas.ecommerce.events.InventoryReservationFailedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentCompletedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentFailedEvent;
import com.ebrahimmorkas.ecommerce.events.Topics;
import com.ebrahimmorkas.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Drives the order to its final state based on the outcome events of the saga. */
@Component
@RequiredArgsConstructor
class OrderSagaListener {

    private final OrderService orderService;

    @KafkaListener(topics = Topics.INVENTORY_RESERVATION_FAILED)
    void onReservationFailed(InventoryReservationFailedEvent event) {
        orderService.cancel(event.orderNumber(), event.reason());
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED)
    void onPaymentCompleted(PaymentCompletedEvent event) {
        orderService.confirm(event.orderNumber());
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    void onPaymentFailed(PaymentFailedEvent event) {
        orderService.cancel(event.orderNumber(), event.reason());
    }
}
