package com.hungrybox.billing;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a payment provider: approves every charge up to a
 * per-order limit and declines anything above it, which makes failures easy to demo.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private final BigDecimal maxTransactionAmount;

    public MockPaymentGateway(@Value("${hungrybox.billing.max-transaction-amount:5000}") BigDecimal maxTransactionAmount) {
        this.maxTransactionAmount = maxTransactionAmount;
    }

    @Override
    public ChargeResult charge(long orderId, long userId, BigDecimal amount) {
        String reference = "PAY-" + shortId();
        if (amount.signum() <= 0) {
            return ChargeResult.declined(reference, "Invalid amount");
        }
        if (amount.compareTo(maxTransactionAmount) > 0) {
            return ChargeResult.declined(reference,
                    "Amount exceeds the per-order limit of " + maxTransactionAmount.toPlainString());
        }
        return ChargeResult.approved(reference);
    }

    @Override
    public String refund(String paymentReference, BigDecimal amount) {
        return "RFD-" + shortId();
    }

    private static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }
}
