package com.ebrahimmorkas.ecommerce.payment.service;

import com.ebrahimmorkas.ecommerce.events.InventoryReservedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentCompletedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentFailedEvent;
import com.ebrahimmorkas.ecommerce.payment.domain.Payment;
import com.ebrahimmorkas.ecommerce.payment.domain.PaymentStatus;
import com.ebrahimmorkas.ecommerce.payment.gateway.PaymentGateway;
import com.ebrahimmorkas.ecommerce.payment.gateway.PaymentGateway.ChargeResult;
import com.ebrahimmorkas.ecommerce.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final UUID ORDER = UUID.randomUUID();
    private static final InventoryReservedEvent EVENT =
            new InventoryReservedEvent(ORDER, "jane@example.com", new BigDecimal("250.00"), Instant.now());

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void approvedChargeStoresCompletedPaymentAndPublishesEvent() {
        given(paymentGateway.charge(ORDER, "jane@example.com", new BigDecimal("250.00")))
                .willReturn(ChargeResult.approved("ref-1"));
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        paymentService.processPayment(EVENT);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        then(paymentRepository).should().save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(saved.getValue().getProviderReference()).isEqualTo("ref-1");
        then(eventPublisher).should().publishEvent(any(PaymentCompletedEvent.class));
    }

    @Test
    void declinedChargeStoresFailedPaymentAndPublishesEvent() {
        given(paymentGateway.charge(any(), any(), any())).willReturn(ChargeResult.declined("Insufficient funds"));

        paymentService.processPayment(EVENT);

        ArgumentCaptor<PaymentFailedEvent> event = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        then(eventPublisher).should().publishEvent(event.capture());
        assertThat(event.getValue().reason()).isEqualTo("Insufficient funds");
    }

    @Test
    void customerIsNeverChargedTwiceForTheSameOrder() {
        given(paymentRepository.existsByOrderNumber(ORDER)).willReturn(true);

        paymentService.processPayment(EVENT);

        then(paymentGateway).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }
}
