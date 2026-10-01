package com.hungrybox.common.events;

/** Kafka topic names. Every event is keyed by orderId so a single order's events stay ordered. */
public final class Topics {

    public static final String ORDER_PLACED = "order.placed";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String PAYMENT_COMPLETED = "payment.completed";

    private Topics() {
    }
}
