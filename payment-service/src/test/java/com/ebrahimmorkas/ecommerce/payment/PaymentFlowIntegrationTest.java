package com.ebrahimmorkas.ecommerce.payment;

import com.ebrahimmorkas.ecommerce.events.InventoryReservedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentCompletedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentFailedEvent;
import com.ebrahimmorkas.ecommerce.events.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "payment.simulator.max-amount=1000.00")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PaymentFlowIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reservedInventoryIsChargedAndPaymentCompletedIsPublished() throws Exception {
        UUID orderNumber = UUID.randomUUID();

        try (var consumer = new KafkaTestConsumer(consumerFactory, Topics.PAYMENT_COMPLETED)) {
            publishInventoryReserved(orderNumber, "999.99");

            PaymentCompletedEvent event = consumer.awaitEvent(orderNumber.toString(), PaymentCompletedEvent.class);
            assertThat(event.amount()).isEqualByComparingTo("999.99");
        }

        mockMvc.perform(get("/api/payments/{orderNumber}", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void chargeAboveLimitPublishesPaymentFailed() throws Exception {
        UUID orderNumber = UUID.randomUUID();

        try (var consumer = new KafkaTestConsumer(consumerFactory, Topics.PAYMENT_FAILED)) {
            publishInventoryReserved(orderNumber, "1500.00");

            PaymentFailedEvent event = consumer.awaitEvent(orderNumber.toString(), PaymentFailedEvent.class);
            assertThat(event.reason()).contains("exceeds limit");
        }

        mockMvc.perform(get("/api/payments/{orderNumber}", orderNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void unknownPaymentReturns404() throws Exception {
        mockMvc.perform(get("/api/payments/{orderNumber}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private void publishInventoryReserved(UUID orderNumber, String amount) {
        kafkaTemplate.send(Topics.INVENTORY_RESERVED, orderNumber.toString(),
                new InventoryReservedEvent(orderNumber, "jane@example.com", new BigDecimal(amount), Instant.now()));
    }
}
