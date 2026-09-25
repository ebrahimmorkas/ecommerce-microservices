package com.ebrahimmorkas.ecommerce.inventory.domain;

import com.ebrahimmorkas.ecommerce.inventory.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private final Product product = new Product("SKU-1", "Phone", null, new BigDecimal("100.00"), 5);

    @Test
    void decreaseStockReducesAvailableQuantity() {
        product.decreaseStock(3);

        assertThat(product.getQuantityAvailable()).isEqualTo(2);
    }

    @Test
    void decreaseStockBeyondAvailableQuantityIsRejected() {
        assertThatThrownBy(() -> product.decreaseStock(6))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 6, available 5");
        assertThat(product.getQuantityAvailable()).isEqualTo(5);
    }

    @Test
    void increaseStockAddsQuantity() {
        product.increaseStock(10);

        assertThat(product.getQuantityAvailable()).isEqualTo(15);
    }
}
