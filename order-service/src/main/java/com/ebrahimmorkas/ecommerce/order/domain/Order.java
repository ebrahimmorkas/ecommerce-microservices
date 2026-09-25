package com.ebrahimmorkas.ecommerce.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, updatable = false)
    private UUID orderNumber;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "failure_reason")
    private String failureReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public Order(String customerEmail) {
        this.orderNumber = UUID.randomUUID();
        this.customerEmail = customerEmail;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addItem(String skuCode, int quantity, BigDecimal unitPrice) {
        items.add(new OrderLineItem(this, skuCode, quantity, unitPrice));
        totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public List<OrderLineItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Transitions are idempotent so that redelivered events are harmless.
     *
     * @return {@code true} if the status changed
     */
    public boolean confirm() {
        return transitionTo(OrderStatus.CONFIRMED, null);
    }

    public boolean cancel(String reason) {
        return transitionTo(OrderStatus.CANCELLED, reason);
    }

    private boolean transitionTo(OrderStatus target, String reason) {
        if (status == target) {
            return false;
        }
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order %s cannot move from %s to %s".formatted(orderNumber, status, target));
        }
        status = target;
        failureReason = reason;
        return true;
    }
}
