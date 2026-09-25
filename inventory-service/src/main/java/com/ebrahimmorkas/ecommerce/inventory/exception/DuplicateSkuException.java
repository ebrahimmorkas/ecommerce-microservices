package com.ebrahimmorkas.ecommerce.inventory.exception;

public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String skuCode) {
        super("Product with SKU '%s' already exists".formatted(skuCode));
    }
}
