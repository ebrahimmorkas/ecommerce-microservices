package com.ebrahimmorkas.ecommerce.notification;

import com.ebrahimmorkas.ecommerce.events.OrderStatusChangedEvent;
import com.ebrahimmorkas.ecommerce.events.Topics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class NotificationIntegrationTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.9.1");

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void orderStatusChangeTriggersCustomerEmail() {
        UUID orderNumber = UUID.randomUUID();

        kafkaTemplate.send(Topics.ORDER_STATUS_CHANGED, orderNumber.toString(), new OrderStatusChangedEvent(
                orderNumber, "jane@example.com", "CONFIRMED", new BigDecimal("699.00"), null, Instant.now()));

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(30_000)).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("jane@example.com");
        assertThat(message.getValue().getSubject()).contains("is confirmed");
    }
}
