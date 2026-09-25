package com.ebrahimmorkas.ecommerce.notification.service;

import com.ebrahimmorkas.ecommerce.events.OrderStatusChangedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderEmailComposerTest {

    private static final UUID ORDER = UUID.fromString("3f2a9c1e-0000-0000-0000-000000000000");

    private final OrderEmailComposer composer = new OrderEmailComposer();

    @Test
    void confirmedEmailContainsShortOrderIdAndAmount() {
        var email = composer.compose(event("CONFIRMED", null));

        assertThat(email.subject()).isEqualTo("Your order #3F2A9C1E is confirmed");
        assertThat(email.body()).contains("$1398.00");
    }

    @Test
    void cancelledEmailContainsReason() {
        var email = composer.compose(event("CANCELLED", "Payment declined"));

        assertThat(email.subject()).isEqualTo("Your order #3F2A9C1E was cancelled");
        assertThat(email.body()).contains("Reason: Payment declined");
    }

    @Test
    void unsupportedStatusIsRejected() {
        assertThatThrownBy(() -> composer.compose(event("PENDING", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static OrderStatusChangedEvent event(String status, String reason) {
        return new OrderStatusChangedEvent(ORDER, "jane@example.com", status, new BigDecimal("1398.00"), reason,
                Instant.now());
    }
}
