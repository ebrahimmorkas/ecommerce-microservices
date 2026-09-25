package com.ebrahimmorkas.ecommerce.order.exception;

public class InventoryUnavailableException extends RuntimeException {

    public InventoryUnavailableException(Throwable cause) {
        super("Inventory service is currently unavailable, please retry later", cause);
    }
}
