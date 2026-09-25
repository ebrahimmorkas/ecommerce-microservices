package com.ebrahimmorkas.ecommerce.inventory.messaging;

import com.ebrahimmorkas.ecommerce.events.OrderCreatedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentFailedEvent;
import com.ebrahimmorkas.ecommerce.events.Topics;
import com.ebrahimmorkas.ecommerce.inventory.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderSagaListener {

    private final StockReservationService reservationService;

    @KafkaListener(topics = Topics.ORDER_CREATED)
    void onOrderCreated(OrderCreatedEvent event) {
        reservationService.reserve(event);
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    void onPaymentFailed(PaymentFailedEvent event) {
        reservationService.release(event.orderNumber());
    }
}
