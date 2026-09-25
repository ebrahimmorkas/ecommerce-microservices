package com.ebrahimmorkas.ecommerce.inventory.dto;

import com.ebrahimmorkas.ecommerce.inventory.domain.Product;

import java.math.BigDecimal;

public record ProductResponse(
        String skuCode,
        String name,
        String description,
        BigDecimal price,
        int quantityAvailable) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getSkuCode(), product.getName(), product.getDescription(),
                product.getPrice(), product.getQuantityAvailable());
    }
}
