package com.ebrahimmorkas.ecommerce.inventory.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String skuCode, int requested, int available) {
        super("Insufficient stock for SKU '%s': requested %d, available %d".formatted(skuCode, requested, available));
    }
}
