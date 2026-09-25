package com.ebrahimmorkas.ecommerce.order.messaging;

import com.ebrahimmorkas.ecommerce.events.InventoryReservationFailedEvent;
import com.ebrahimmorkas.ecommerce.events.OrderCreatedEvent;
import com.ebrahimmorkas.ecommerce.events.OrderStatusChangedEvent;
import com.ebrahimmorkas.ecommerce.events.PaymentCompletedEvent;
import com.ebrahimmorkas.ecommerce.events.Topics;
import com.ebrahimmorkas.ecommerce.order.KafkaTestConsumer;
import com.ebrahimmorkas.ecommerce.order.TestcontainersConfiguration;
import com.ebrahimmorkas.ecommerce.order.client.InventoryClient;
import com.ebrahimmorkas.ecommerce.order.client.ProductInfo;
import com.ebrahimmorkas.ecommerce.order.domain.OrderStatus;
import com.ebrahimmorkas.ecommerce.order.dto.CreateOrderRequest;
import com.ebrahimmorkas.ecommerce.order.dto.OrderResponse;
import com.ebrahimmorkas.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OrderSagaIntegrationTest {

    @MockitoBean
    private InventoryClient inventoryClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    @BeforeEach
    void stubCatalog() {
        given(inventoryClient.findProducts(anyCollection()))
                .willReturn(List.of(new ProductInfo("PIXEL-9", "Pixel 9", new BigDecimal("699.00"), 30)));
    }

    @Test
    void placedOrderIsPublishedAndConfirmedWhenPaymentCompletes() {
        try (var consumer = new KafkaTestConsumer(consumerFactory, Topics.ORDER_CREATED, Topics.ORDER_STATUS_CHANGED)) {
            UUID orderNumber = placeOrder().orderNumber();

            OrderCreatedEvent created = consumer.awaitEvent(orderNumber.toString(), OrderCreatedEvent.class);
            assertThat(created.items()).containsExactly(new OrderCreatedEvent.Item("PIXEL-9", 2));
            assertThat(created.totalAmount()).isEqualByComparingTo("1398.00");

            kafkaTemplate.send(Topics.PAYMENT_COMPLETED, orderNumber.toString(),
                    new PaymentCompletedEvent(orderNumber, UUID.randomUUID(), created.totalAmount(), Instant.now()));

            awaitStatus(orderNumber, OrderStatus.CONFIRMED);
            OrderStatusChangedEvent changed = consumer.awaitEvent(orderNumber.toString(), OrderStatusChangedEvent.class);
            assertThat(changed.status()).isEqualTo("CONFIRMED");
        }
    }

    @Test
    void orderIsCancelledWhenStockCannotBeReserved() {
        UUID orderNumber = placeOrder().orderNumber();

        kafkaTemplate.send(Topics.INVENTORY_RESERVATION_FAILED, orderNumber.toString(),
                new InventoryReservationFailedEvent(orderNumber, "Insufficient stock for PIXEL-9", Instant.now()));

        awaitStatus(orderNumber, OrderStatus.CANCELLED);
        assertThat(orderService.findByOrderNumber(orderNumber).failureReason()).contains("Insufficient stock");
    }

    private OrderResponse placeOrder() {
        return orderService.placeOrder(new CreateOrderRequest("jane@example.com",
                List.of(new CreateOrderRequest.Item("PIXEL-9", 2))));
    }

    private void awaitStatus(UUID orderNumber, OrderStatus expected) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(orderService.findByOrderNumber(orderNumber).status()).isEqualTo(expected));
    }
}
