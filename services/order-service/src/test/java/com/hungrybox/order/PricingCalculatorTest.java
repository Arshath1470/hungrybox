package com.hungrybox.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hungrybox.order.PricingCalculator.Line;
import com.hungrybox.order.PricingCalculator.PriceBreakdown;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void smallOrderPaysDeliveryAndGst() {
        PriceBreakdown price = calculator.price(List.of(
                new Line(new BigDecimal("120.00"), 2),
                new Line(new BigDecimal("90.00"), 1)));

        assertThat(price.subtotal()).isEqualByComparingTo("330.00");
        assertThat(price.deliveryFee()).isEqualByComparingTo("40.00");
        assertThat(price.tax()).isEqualByComparingTo("16.50");
        assertThat(price.total()).isEqualByComparingTo("386.50");
    }

    @ParameterizedTest
    @CsvSource({
            "499.99, 40.00",
            "500.00, 0.00",
            "1200.00, 0.00"
    })
    void deliveryIsFreeFromThreshold(String subtotal, String expectedFee) {
        PriceBreakdown price = calculator.price(List.of(new Line(new BigDecimal(subtotal), 1)));
        assertThat(price.deliveryFee()).isEqualByComparingTo(expectedFee);
    }

    @Test
    void gstIsRoundedHalfUp() {
        PriceBreakdown price = calculator.price(List.of(new Line(new BigDecimal("10.10"), 1)));
        // 5% of 10.10 = 0.505 -> 0.51
        assertThat(price.tax()).isEqualByComparingTo("0.51");
    }

    @Test
    void emptyOrderIsRejected() {
        assertThatThrownBy(() -> calculator.price(List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
