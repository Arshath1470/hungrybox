package com.hungrybox.order;

/**
 * Order lifecycle. Payment moves an order out of PENDING_PAYMENT; the kitchen and
 * delivery steps are driven by admins; customers may cancel until the kitchen starts.
 *
 * <pre>
 * PENDING_PAYMENT ──► CONFIRMED ──► PREPARING ──► OUT_FOR_DELIVERY ──► DELIVERED
 *        │                │
 *        ▼                ▼
 *  PAYMENT_FAILED     CANCELLED (refund issued by billing-service)
 * </pre>
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    PAYMENT_FAILED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING_PAYMENT -> next == CONFIRMED || next == PAYMENT_FAILED;
            case CONFIRMED -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> next == DELIVERED;
            case DELIVERED, PAYMENT_FAILED, CANCELLED -> false;
        };
    }

    public boolean isCancellableByCustomer() {
        return this == CONFIRMED;
    }
}
