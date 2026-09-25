package com.ebrahimmorkas.ecommerce.payment.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID orderNumber) {
        super("No payment found for order '%s'".formatted(orderNumber));
    }
}
