package com.hungrybox.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.hungrybox.billing.PaymentGateway.ChargeResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway(new BigDecimal("5000"));

    @Test
    void approvesWithinLimit() {
        ChargeResult result = gateway.charge(1L, 1L, new BigDecimal("4999.99"));
        assertThat(result.approved()).isTrue();
        assertThat(result.reference()).startsWith("PAY-");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void declinesAboveLimit() {
        ChargeResult result = gateway.charge(1L, 1L, new BigDecimal("5000.01"));
        assertThat(result.approved()).isFalse();
        assertThat(result.failureReason()).contains("limit");
    }

    @Test
    void declinesNonPositiveAmounts() {
        assertThat(gateway.charge(1L, 1L, BigDecimal.ZERO).approved()).isFalse();
    }

    @Test
    void invoiceNumbersAreZeroPadded() {
        assertThat(BillingService.invoiceNumber(42L)).matches("HB-\\d{8}-000042");
    }
}
