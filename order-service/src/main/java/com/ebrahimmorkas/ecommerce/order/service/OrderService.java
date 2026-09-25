package com.ebrahimmorkas.ecommerce.order.service;

import com.ebrahimmorkas.ecommerce.order.client.InventoryClient;
import com.ebrahimmorkas.ecommerce.order.client.ProductInfo;
import com.ebrahimmorkas.ecommerce.order.domain.Order;
import com.ebrahimmorkas.ecommerce.order.dto.CreateOrderRequest;
import com.ebrahimmorkas.ecommerce.order.dto.OrderResponse;
import com.ebrahimmorkas.ecommerce.order.exception.OrderNotFoundException;
import com.ebrahimmorkas.ecommerce.order.exception.UnknownProductException;
import com.ebrahimmorkas.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    /**
     * Prices the order against the live catalog and stores it as PENDING. Stock is not checked
     * here: reservation happens asynchronously so a slow inventory never blocks checkout.
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
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse findByOrderNumber(UUID orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findByCustomer(String customerEmail, Pageable pageable) {
        return orderRepository.findByCustomerEmailIgnoreCase(customerEmail, pageable).map(OrderResponse::from);
    }
}
