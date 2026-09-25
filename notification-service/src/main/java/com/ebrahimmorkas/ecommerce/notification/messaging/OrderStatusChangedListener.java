package com.ebrahimmorkas.ecommerce.notification.messaging;

import com.ebrahimmorkas.ecommerce.events.OrderStatusChangedEvent;
import com.ebrahimmorkas.ecommerce.events.Topics;
import com.ebrahimmorkas.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class OrderStatusChangedListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = Topics.ORDER_STATUS_CHANGED)
    void onOrderStatusChanged(OrderStatusChangedEvent event) {
        notificationService.notifyCustomer(event);
    }
}
