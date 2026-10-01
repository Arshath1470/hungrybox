package com.hungrybox.common.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.Instant;

/** Published by billing-service after attempting to charge an order. */
public record PaymentCompletedEvent(
        long orderId,
        long userId,
        String status,
        String invoiceNumber,
        BigDecimal amount,
        String failureReason,
        Instant processedAt) {

    public static final String PAID = "PAID";
    public static final String FAILED = "FAILED";

    @JsonIgnore
    public boolean isPaid() {
        return PAID.equals(status);
    }
}
