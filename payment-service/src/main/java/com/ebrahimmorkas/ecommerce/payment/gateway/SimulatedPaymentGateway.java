package com.ebrahimmorkas.ecommerce.payment.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deterministic stand-in for a real provider: approves charges up to a configurable limit and
 * declines anything above it, which makes both saga paths easy to demo.
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    private final Properties properties;

    public SimulatedPaymentGateway(Properties properties) {
        this.properties = properties;
    }

    @Override
    public ChargeResult charge(UUID orderNumber, String customerEmail, BigDecimal amount) {
        if (amount.compareTo(properties.maxAmount()) > 0) {
            return ChargeResult.declined("Payment declined: amount %s exceeds limit %s"
                    .formatted(amount.toPlainString(), properties.maxAmount().toPlainString()));
        }
        return ChargeResult.approved("sim_" + UUID.randomUUID());
    }

    @ConfigurationProperties("payment.simulator")
    public record Properties(BigDecimal maxAmount) {
    }
}
