package com.ebrahimmorkas.ecommerce.payment.service;

import com.ebrahimmorkas.ecommerce.events.InventoryReservedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentCompletedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentFailedEvent;
import com.ebrahimmorkas.ecommerce.payment.domain.Payment;
import com.ebrahimmorkas.ecommerce.payment.dto.PaymentResponse;
import com.ebrahimmorkas.ecommerce.payment.exception.PaymentNotFoundException;
import com.ebrahimmorkas.ecommerce.payment.gateway.PaymentGateway;
import com.ebrahimmorkas.ecommerce.payment.gateway.PaymentGateway.ChargeResult;
import com.ebrahimmorkas.ecommerce.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;

    /** Charges the customer once stock is reserved. Redelivered events are ignored. */
    @Transactional
    public void processPayment(InventoryReservedEvent event) {
        UUID orderNumber = event.orderNumber();
        if (paymentRepository.existsByOrderNumber(orderNumber)) {
            log.info("Ignoring duplicate InventoryReservedEvent for order {}", orderNumber);
            return;
        }

        ChargeResult result = paymentGateway.charge(orderNumber, event.customerEmail(), event.totalAmount());
        if (result.approved()) {
            Payment payment = paymentRepository.save(
                    Payment.completed(orderNumber, event.totalAmount(), result.providerReference()));
            log.info("Payment {} completed for order {}", payment.getId(), orderNumber);
            eventPublisher.publishEvent(new PaymentCompletedEvent(orderNumber, payment.getId(),
                    payment.getAmount(), Instant.now()));
        } else {
            paymentRepository.save(Payment.failed(orderNumber, event.totalAmount(), result.declineReason()));
            log.info("Payment failed for order {}: {}", orderNumber, result.declineReason());
            eventPublisher.publishEvent(new PaymentFailedEvent(orderNumber, result.declineReason(), Instant.now()));
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse findByOrderNumber(UUID orderNumber) {
        return paymentRepository.findByOrderNumber(orderNumber)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException(orderNumber));
    }
}
