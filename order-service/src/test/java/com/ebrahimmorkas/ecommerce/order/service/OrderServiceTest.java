package com.ebrahimmorkas.ecommerce.order.service;

import com.ebrahimmorkas.ecommerce.order.client.InventoryClient;
import com.ebrahimmorkas.ecommerce.order.client.ProductInfo;
import com.ebrahimmorkas.ecommerce.order.domain.Order;
import com.ebrahimmorkas.ecommerce.order.dto.CreateOrderRequest;
import com.ebrahimmorkas.ecommerce.order.dto.CreateOrderRequest.Item;
import com.ebrahimmorkas.ecommerce.order.exception.UnknownProductException;
import com.ebrahimmorkas.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

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

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrderPricesItemsFromCatalogAndMergesDuplicateSkus() {
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
    }
}
