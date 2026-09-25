package com.ebrahimmorkas.ecommerce.notification.service;

import com.ebrahimmorkas.ecommerce.events.OrderStatusChangedEvent;
import org.springframework.stereotype.Component;

/** Builds the subject and body of customer emails for order status changes. */
@Component
public class OrderEmailComposer {

    public Email compose(OrderStatusChangedEvent event) {
        return switch (event.status()) {
            case "CONFIRMED" -> new Email(
                    "Your order %s is confirmed".formatted(shortId(event)),
                    """
                            Hi,

                            Good news! Your order %s has been confirmed and your payment of $%s was received.
                            We'll let you know when it ships.

                            Thanks for shopping with us.
                            """.formatted(event.orderNumber(), event.totalAmount().toPlainString()));
            case "CANCELLED" -> new Email(
                    "Your order %s was cancelled".formatted(shortId(event)),
                    """
                            Hi,

                            Unfortunately we couldn't complete your order %s.
                            Reason: %s

                            You have not been charged. Please try again or contact support.
                            """.formatted(event.orderNumber(), event.reason()));
            default -> throw new IllegalArgumentException("No email template for status " + event.status());
        };
    }

    private static String shortId(OrderStatusChangedEvent event) {
        return "#" + event.orderNumber().toString().substring(0, 8).toUpperCase();
    }

    public record Email(String subject, String body) {
    }
}
