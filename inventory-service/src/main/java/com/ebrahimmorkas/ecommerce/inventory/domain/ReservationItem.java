package com.ebrahimmorkas.ecommerce.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ReservationItem(
        @Column(name = "sku_code", nullable = false) String skuCode,
        @Column(nullable = false) int quantity) {
}
