package com.ebrahimmorkas.ecommerce.payment.messaging;

import com.ebrahimmorkas.ecommerce.events.InventoryReservedEvent;
import com.ebrahimmorkas.ecommerce.events.Topics;
import com.ebrahimmorkas.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class InventoryReservedListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = Topics.INVENTORY_RESERVED)
    void onInventoryReserved(InventoryReservedEvent event) {
        paymentService.processPayment(event);
    }
}
