package com.hungrybox.common.events;

import java.time.Instant;

/** Published by order-service when a paid order is cancelled; billing-service refunds it. */
public record OrderCancelledEvent(long orderId, long userId, String reason, Instant cancelledAt) {
}
