package com.ebrahimmorkas.ecommerce.inventory.messaging;

import com.ebrahimmorkas.ecommerce.events.InventoryReservationFailedEvent;
import com.ebrahimmorkas.ecommerce.events.InventoryReservedEvent;
import com.ebrahimmorkas.ecommerce.events.OrderCreatedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentFailedEvent;
import com.ebrahimmorkas.ecommerce.events.Topics;
import com.ebrahimmorkas.ecommerce.inventory.KafkaTestConsumer;
import com.ebrahimmorkas.ecommerce.inventory.TestcontainersConfiguration;
import com.ebrahimmorkas.ecommerce.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OrderSagaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void orderCreatedReservesStockAndPublishesInventoryReserved() {
        int before = stockOf("GALAXY-S24");
        UUID orderNumber = UUID.randomUUID();

        try (var consumer = new KafkaTestConsumer(consumerFactory, Topics.INVENTORY_RESERVED)) {
            publishOrderCreated(orderNumber, "GALAXY-S24", 2);

            InventoryReservedEvent event = consumer.awaitEvent(orderNumber.toString(), InventoryReservedEvent.class);

            assertThat(event.customerEmail()).isEqualTo("jane@example.com");
            assertThat(event.totalAmount()).isEqualByComparingTo("1698.00");
        }
        assertThat(stockOf("GALAXY-S24")).isEqualTo(before - 2);
    }

    @Test
    void insufficientStockPublishesReservationFailedAndLeavesStockUntouched() {
        int before = stockOf("MACBOOK-AIR");
        UUID orderNumber = UUID.randomUUID();

        try (var consumer = new KafkaTestConsumer(consumerFactory, Topics.INVENTORY_RESERVATION_FAILED)) {
            publishOrderCreated(orderNumber, "MACBOOK-AIR", 1_000);

            InventoryReservationFailedEvent event =
                    consumer.awaitEvent(orderNumber.toString(), InventoryReservationFailedEvent.class);

            assertThat(event.reason()).contains("Insufficient stock for MACBOOK-AIR");
        }
        assertThat(stockOf("MACBOOK-AIR")).isEqualTo(before);
    }

    @Test
    void paymentFailureReleasesReservedStock() {
        int before = stockOf("AIRPODS-PRO");
        UUID orderNumber = UUID.randomUUID();

        try (var consumer = new KafkaTestConsumer(consumerFactory, Topics.INVENTORY_RESERVED)) {
            publishOrderCreated(orderNumber, "AIRPODS-PRO", 3);
            consumer.awaitEvent(orderNumber.toString(), InventoryReservedEvent.class);
        }
        assertThat(stockOf("AIRPODS-PRO")).isEqualTo(before - 3);

        kafkaTemplate.send(Topics.PAYMENT_FAILED, orderNumber.toString(),
                new PaymentFailedEvent(orderNumber, "Card declined", Instant.now()));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(stockOf("AIRPODS-PRO")).isEqualTo(before));
    }

    private void publishOrderCreated(UUID orderNumber, String skuCode, int quantity) {
        BigDecimal price = productRepository.findBySkuCode(skuCode).orElseThrow().getPrice();
        kafkaTemplate.send(Topics.ORDER_CREATED, orderNumber.toString(), new OrderCreatedEvent(
                orderNumber, "jane@example.com", List.of(new OrderCreatedEvent.Item(skuCode, quantity)),
                price.multiply(BigDecimal.valueOf(quantity)), Instant.now()));
    }

    private int stockOf(String skuCode) {
        return productRepository.findBySkuCode(skuCode).orElseThrow().getQuantityAvailable();
    }
}
