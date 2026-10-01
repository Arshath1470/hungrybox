package com.hungrybox.common.events;

import java.math.BigDecimal;
import java.time.Instant;

/** Published by order-service once an order is persisted; billing-service charges it. */
public record OrderPlacedEvent(
        long orderId,
        long userId,
        long restaurantId,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal tax,
        BigDecimal total,
        Instant placedAt) {
}
