package com.hungrybox.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class BillingDtos {

    private BillingDtos() {
    }

    public enum InvoiceStatus { PAID, FAILED, REFUNDED }

    public record Invoice(
            long id,
            String invoiceNumber,
            long orderId,
            long userId,
            long restaurantId,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal tax,
            BigDecimal total,
            InvoiceStatus status,
            String paymentReference,
            String refundReference,
            String failureReason,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record DailyRevenue(LocalDate date, long paidOrders, BigDecimal revenue) {
    }

    /** Leadership-style billing snapshot: volumes, collections, refunds and a 7-day trend. */
    public record BillingSummary(
            long totalInvoices,
            long paidCount,
            long failedCount,
            long refundedCount,
            BigDecimal collected,
            BigDecimal refunded,
            BigDecimal netRevenue,
            List<DailyRevenue> last7Days,
            Instant generatedAt) {
    }
}
