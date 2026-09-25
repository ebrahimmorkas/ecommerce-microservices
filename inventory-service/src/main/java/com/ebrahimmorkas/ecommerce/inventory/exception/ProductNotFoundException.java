package com.ebrahimmorkas.ecommerce.inventory.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String skuCode) {
        super("Product with SKU '%s' not found".formatted(skuCode));
    }
}
