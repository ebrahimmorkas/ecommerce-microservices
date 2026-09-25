package com.ebrahimmorkas.ecommerce.order.dto;

import com.ebrahimmorkas.ecommerce.order.domain.Order;
import com.ebrahimmorkas.ecommerce.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderNumber,
        String customerEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        String failureReason,
        List<Item> items,
        Instant createdAt) {

    public record Item(String skuCode, int quantity, BigDecimal unitPrice) {
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(i -> new Item(i.getSkuCode(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderResponse(order.getOrderNumber(), order.getCustomerEmail(), order.getStatus(),
                order.getTotalAmount(), order.getFailureReason(), items, order.getCreatedAt());
    }
}
