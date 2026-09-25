package com.ebrahimmorkas.ecommerce.order.client;

import java.math.BigDecimal;

/** Subset of the inventory-service product representation the order service depends on. */
public record ProductInfo(String skuCode, String name, BigDecimal price, int quantityAvailable) {
}
