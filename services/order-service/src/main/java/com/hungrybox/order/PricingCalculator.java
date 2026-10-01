package com.hungrybox.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/** Server-side pricing: subtotal + delivery fee (free above a threshold) + 5% GST. */
@Component
public class PricingCalculator {

    static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("500.00");
    static final BigDecimal DELIVERY_FEE = new BigDecimal("40.00");
    static final BigDecimal GST_RATE = new BigDecimal("0.05");

    public PriceBreakdown price(List<Line> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one line");
        }
        BigDecimal subtotal = lines.stream()
                .map(Line::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal deliveryFee = subtotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0
                ? BigDecimal.ZERO.setScale(2)
                : DELIVERY_FEE;
        BigDecimal tax = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        return new PriceBreakdown(subtotal, deliveryFee, tax, subtotal.add(deliveryFee).add(tax));
    }

    public record Line(BigDecimal unitPrice, int quantity) {
        public BigDecimal lineTotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    public record PriceBreakdown(BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal tax, BigDecimal total) {
    }
}
