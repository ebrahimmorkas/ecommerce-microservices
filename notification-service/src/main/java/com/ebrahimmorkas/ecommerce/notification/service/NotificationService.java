package com.ebrahimmorkas.ecommerce.notification.service;

import com.ebrahimmorkas.ecommerce.events.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final OrderEmailComposer composer;

    @Value("${notification.from-address}")
    private String fromAddress;

    public void notifyCustomer(OrderStatusChangedEvent event) {
        OrderEmailComposer.Email email = composer.compose(event);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(event.customerEmail());
        message.setSubject(email.subject());
        message.setText(email.body());
        mailSender.send(message);

        log.info("Sent {} email for order {}", event.status(), event.orderNumber());
    }
}
