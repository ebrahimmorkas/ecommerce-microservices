package com.ebrahimmorkas.ecommerce.order.service;

import com.ebrahimmorkas.ecommerce.events.OrderCreatedEvent;
import com.ebrahimmorkas.ecommerce.events.OrderStatusChangedEvent;
import com.ebrahimmorkas.ecommerce.order.client.InventoryClient;
import com.ebrahimmorkas.ecommerce.order.client.ProductInfo;
import com.ebrahimmorkas.ecommerce.order.domain.Order;
import com.ebrahimmorkas.ecommerce.order.dto.CreateOrderRequest;
import com.ebrahimmorkas.ecommerce.order.dto.OrderResponse;
import com.ebrahimmorkas.ecommerce.order.exception.OrderNotFoundException;
import com.ebrahimmorkas.ecommerce.order.exception.UnknownProductException;
import com.ebrahimmorkas.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Prices the order against the live catalog and stores it as PENDING. Stock reservation and
     * payment happen asynchronously (saga), so a slow downstream service never blocks checkout.
     */
    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        Map<String, Integer> quantitiesBySku = request.items().stream()
                .collect(Collectors.toMap(CreateOrderRequest.Item::skuCode, CreateOrderRequest.Item::quantity,
                        Integer::sum, LinkedHashMap::new));

        Map<String, ProductInfo> products = inventoryClient.findProducts(quantitiesBySku.keySet()).stream()
                .collect(Collectors.toMap(ProductInfo::skuCode, Function.identity()));

        List<String> unknown = quantitiesBySku.keySet().stream().filter(sku -> !products.containsKey(sku)).toList();
        if (!unknown.isEmpty()) {
            throw new UnknownProductException(unknown);
        }

        Order order = new Order(request.customerEmail());
        quantitiesBySku.forEach((sku, quantity) -> order.addItem(sku, quantity, products.get(sku).price()));
        Order saved = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderCreatedEvent(saved.getOrderNumber(), saved.getCustomerEmail(),
                saved.getItems().stream().map(i -> new OrderCreatedEvent.Item(i.getSkuCode(), i.getQuantity())).toList(),
                saved.getTotalAmount(), Instant.now()));
        return OrderResponse.from(saved);
    }

    @Transactional
    public void confirm(UUID orderNumber) {
        Order order = getOrder(orderNumber);
        if (order.confirm()) {
            log.info("Order {} confirmed", orderNumber);
            publishStatusChanged(order);
        }
    }

    @Transactional
    public void cancel(UUID orderNumber, String reason) {
        Order order = getOrder(orderNumber);
        if (order.cancel(reason)) {
            log.info("Order {} cancelled: {}", orderNumber, reason);
            publishStatusChanged(order);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse findByOrderNumber(UUID orderNumber) {
        return OrderResponse.from(getOrder(orderNumber));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findByCustomer(String customerEmail, Pageable pageable) {
        return orderRepository.findByCustomerEmailIgnoreCase(customerEmail, pageable).map(OrderResponse::from);
    }

    private Order getOrder(UUID orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    private void publishStatusChanged(Order order) {
        eventPublisher.publishEvent(new OrderStatusChangedEvent(order.getOrderNumber(), order.getCustomerEmail(),
                order.getStatus().name(), order.getTotalAmount(), order.getFailureReason(), Instant.now()));
    }
}
