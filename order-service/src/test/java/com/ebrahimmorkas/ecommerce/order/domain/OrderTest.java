package com.ebrahimmorkas.ecommerce.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void totalIsSumOfLineItems() {
        Order order = new Order("jane@example.com");
        order.addItem("A", 2, new BigDecimal("10.50"));
        order.addItem("B", 1, new BigDecimal("5.00"));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("26.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void confirmIsIdempotent() {
        Order order = new Order("jane@example.com");

        assertThat(order.confirm()).isTrue();
        assertThat(order.confirm()).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void cancelRecordsReason() {
        Order order = new Order("jane@example.com");

        order.cancel("Payment declined");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getFailureReason()).isEqualTo("Payment declined");
    }

    @Test
    void confirmedOrderCannotBeCancelled() {
        Order order = new Order("jane@example.com");
        order.confirm();

        assertThatThrownBy(() -> order.cancel("late failure")).isInstanceOf(IllegalStateException.class);
    }
}
