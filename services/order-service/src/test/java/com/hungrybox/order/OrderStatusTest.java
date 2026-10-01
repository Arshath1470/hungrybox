package com.hungrybox.order;

import static com.hungrybox.order.OrderStatus.CANCELLED;
import static com.hungrybox.order.OrderStatus.CONFIRMED;
import static com.hungrybox.order.OrderStatus.DELIVERED;
import static com.hungrybox.order.OrderStatus.OUT_FOR_DELIVERY;
import static com.hungrybox.order.OrderStatus.PAYMENT_FAILED;
import static com.hungrybox.order.OrderStatus.PENDING_PAYMENT;
import static com.hungrybox.order.OrderStatus.PREPARING;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertThat(PENDING_PAYMENT.canTransitionTo(CONFIRMED)).isTrue();
        assertThat(CONFIRMED.canTransitionTo(PREPARING)).isTrue();
        assertThat(PREPARING.canTransitionTo(OUT_FOR_DELIVERY)).isTrue();
        assertThat(OUT_FOR_DELIVERY.canTransitionTo(DELIVERED)).isTrue();
    }

    @Test
    void cannotSkipSteps() {
        assertThat(PENDING_PAYMENT.canTransitionTo(PREPARING)).isFalse();
        assertThat(CONFIRMED.canTransitionTo(DELIVERED)).isFalse();
        assertThat(PREPARING.canTransitionTo(CANCELLED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "PAYMENT_FAILED", "CANCELLED"})
    void terminalStatesAreFinal(OrderStatus terminal) {
        for (OrderStatus next : OrderStatus.values()) {
            assertThat(terminal.canTransitionTo(next)).isFalse();
        }
    }

    @Test
    void onlyConfirmedOrdersAreCustomerCancellable() {
        for (OrderStatus status : OrderStatus.values()) {
            assertThat(status.isCancellableByCustomer()).isEqualTo(status == CONFIRMED);
        }
        assertThat(PAYMENT_FAILED.isCancellableByCustomer()).isFalse();
    }
}
