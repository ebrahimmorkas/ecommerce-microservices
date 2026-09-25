package com.ebrahimmorkas.ecommerce.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

/** Port to an external payment provider (Stripe, Adyen, ...). */
public interface PaymentGateway {

    ChargeResult charge(UUID orderNumber, String customerEmail, BigDecimal amount);

    record ChargeResult(boolean approved, String providerReference, String declineReason) {

        public static ChargeResult approved(String providerReference) {
            return new ChargeResult(true, providerReference, null);
        }

        public static ChargeResult declined(String reason) {
            return new ChargeResult(false, null, reason);
        }
    }
}
