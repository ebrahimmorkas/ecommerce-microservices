package com.ebrahimmorkas.ecommerce.order.service;

import com.ebrahimmorkas.ecommerce.events.OrderCreatedEvent;
import com.ebrahimmorkas.ecommerce.events.OrderStatusChangedEvent;
import com.ebrahimmorkas.ecommerce.order.client.InventoryClient;
import com.ebrahimmorkas.ecommerce.order.client.ProductInfo;
import com.ebrahimmorkas.ecommerce.order.domain.Order;
import com.ebrahimmorkas.ecommerce.order.domain.OrderStatus;
import com.ebrahimmorkas.ecommerce.order.dto.CreateOrderRequest;
import com.ebrahimmorkas.ecommerce.order.dto.CreateOrderRequest.Item;
import com.ebrahimmorkas.ecommerce.order.exception.UnknownProductException;
import com.ebrahimmorkas.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrderPricesItemsFromCatalogMergesDuplicateSkusAndPublishesEvent() {
        given(inventoryClient.findProducts(anyCollection())).willReturn(List.of(
                new ProductInfo("PHONE", "Phone", new BigDecimal("500.00"), 10),
                new ProductInfo("CASE", "Case", new BigDecimal("20.00"), 10)));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        var response = orderService.placeOrder(new CreateOrderRequest("jane@example.com", List.of(
                new Item("PHONE", 1), new Item("CASE", 1), new Item("CASE", 2))));

        assertThat(response.totalAmount()).isEqualByComparingTo("560.00");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items()).anySatisfy(item -> {
            assertThat(item.skuCode()).isEqualTo("CASE");
            assertThat(item.quantity()).isEqualTo(3);
        });

        ArgumentCaptor<OrderCreatedEvent> event = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        then(eventPublisher).should().publishEvent(event.capture());
        assertThat(event.getValue().orderNumber()).isEqualTo(response.orderNumber());
        assertThat(event.getValue().items()).containsExactly(
                new OrderCreatedEvent.Item("PHONE", 1), new OrderCreatedEvent.Item("CASE", 3));
    }

    @Test
    void placeOrderRejectsUnknownSku() {
        given(inventoryClient.findProducts(anyCollection())).willReturn(List.of(
                new ProductInfo("PHONE", "Phone", new BigDecimal("500.00"), 10)));

        assertThatThrownBy(() -> orderService.placeOrder(new CreateOrderRequest("jane@example.com", List.of(
                new Item("PHONE", 1), new Item("GHOST", 1)))))
                .isInstanceOf(UnknownProductException.class)
                .hasMessageContaining("GHOST");
        then(orderRepository).should(never()).save(any());
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void confirmPublishesStatusChangeOnlyOnce() {
        Order order = new Order("jane@example.com");
        given(orderRepository.findByOrderNumber(order.getOrderNumber())).willReturn(Optional.of(order));

        orderService.confirm(order.getOrderNumber());
        orderService.confirm(order.getOrderNumber());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        then(eventPublisher).should().publishEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void cancelStoresReasonAndPublishesStatusChange() {
        Order order = new Order("jane@example.com");
        given(orderRepository.findByOrderNumber(order.getOrderNumber())).willReturn(Optional.of(order));

        orderService.cancel(order.getOrderNumber(), "Payment declined");

        assertThat(order.getFailureReason()).isEqualTo("Payment declined");
        ArgumentCaptor<OrderStatusChangedEvent> event = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        then(eventPublisher).should().publishEvent(event.capture());
        assertThat(event.getValue().status()).isEqualTo("CANCELLED");
        assertThat(event.getValue().reason()).isEqualTo("Payment declined");
    }
}
