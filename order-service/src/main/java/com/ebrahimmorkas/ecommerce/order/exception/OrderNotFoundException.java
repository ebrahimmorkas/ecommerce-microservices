package com.ebrahimmorkas.ecommerce.order.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderNumber) {
        super("Order '%s' not found".formatted(orderNumber));
    }
}
