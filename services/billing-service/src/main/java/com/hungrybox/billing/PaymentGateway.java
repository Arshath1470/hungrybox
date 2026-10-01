package com.hungrybox.billing;

import java.math.BigDecimal;

/** Abstraction over a payment provider so a real gateway can replace the mock without touching billing logic. */
public interface PaymentGateway {

    /** Implementations must be idempotent on orderId so a redelivered event never double-charges. */
    ChargeResult charge(long orderId, long userId, BigDecimal amount);

    String refund(String paymentReference, BigDecimal amount);

    record ChargeResult(boolean approved, String reference, String failureReason) {

        static ChargeResult approved(String reference) {
            return new ChargeResult(true, reference, null);
        }

        static ChargeResult declined(String reference, String reason) {
            return new ChargeResult(false, reference, reason);
        }
    }
}
