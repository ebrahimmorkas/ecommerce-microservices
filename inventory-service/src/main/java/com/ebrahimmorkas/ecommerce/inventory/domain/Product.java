package com.ebrahimmorkas.ecommerce.inventory.domain;

import com.ebrahimmorkas.ecommerce.inventory.exception.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_code", nullable = false, unique = true, updatable = false)
    private String skuCode;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    /** Optimistic locking guards against lost updates when concurrent orders reserve the same SKU. */
    @Version
    private long version;

    public Product(String skuCode, String name, String description, BigDecimal price, int quantityAvailable) {
        this.skuCode = skuCode;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantityAvailable = quantityAvailable;
    }

    public void decreaseStock(int quantity) {
        if (quantity > quantityAvailable) {
            throw new InsufficientStockException(skuCode, quantity, quantityAvailable);
        }
        quantityAvailable -= quantity;
    }

    public void increaseStock(int quantity) {
        quantityAvailable += quantity;
    }
}
