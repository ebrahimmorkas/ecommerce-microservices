package com.ebrahimmorkas.ecommerce.payment.gateway;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayTest {

    private final SimulatedPaymentGateway gateway =
            new SimulatedPaymentGateway(new SimulatedPaymentGateway.Properties(new BigDecimal("1000.00")));

    @Test
    void approvesAmountsUpToTheLimit() {
        var result = gateway.charge(UUID.randomUUID(), "jane@example.com", new BigDecimal("1000.00"));

        assertThat(result.approved()).isTrue();
        assertThat(result.providerReference()).startsWith("sim_");
    }

    @Test
    void declinesAmountsAboveTheLimit() {
        var result = gateway.charge(UUID.randomUUID(), "jane@example.com", new BigDecimal("1000.01"));

        assertThat(result.approved()).isFalse();
        assertThat(result.declineReason()).contains("exceeds limit 1000.00");
    }
}
