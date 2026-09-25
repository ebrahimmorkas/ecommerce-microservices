package com.ebrahimmorkas.ecommerce.order.web;

import com.ebrahimmorkas.ecommerce.order.dto.CreateOrderRequest;
import com.ebrahimmorkas.ecommerce.order.dto.OrderResponse;
import com.ebrahimmorkas.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Place and track orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place an order", description = "Order is created as PENDING and processed asynchronously")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.placeOrder(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{orderNumber}").buildAndExpand(order.orderNumber()).toUri();
        return ResponseEntity.created(location).body(order);
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get an order by its order number")
    public OrderResponse findByOrderNumber(@PathVariable UUID orderNumber) {
        return orderService.findByOrderNumber(orderNumber);
    }

    @GetMapping
    @Operation(summary = "List a customer's orders, newest first")
    public Page<OrderResponse> findByCustomer(
            @RequestParam String customerEmail,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.findByCustomer(customerEmail, pageable);
    }
}
