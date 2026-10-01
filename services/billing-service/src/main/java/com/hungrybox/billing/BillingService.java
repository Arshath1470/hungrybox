package com.hungrybox.billing;

import com.hungrybox.billing.BillingDtos.BillingSummary;
import com.hungrybox.billing.BillingDtos.Invoice;
import com.hungrybox.billing.BillingDtos.InvoiceStatus;
import com.hungrybox.billing.InvoiceRepository.StatusTotals;
import com.hungrybox.billing.PaymentGateway.ChargeResult;
import com.hungrybox.common.events.OrderCancelledEvent;
import com.hungrybox.common.events.OrderPlacedEvent;
import com.hungrybox.common.events.PaymentCompletedEvent;
import com.hungrybox.common.security.AuthenticatedUser;
import com.hungrybox.common.web.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final DateTimeFormatter INVOICE_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int LIST_LIMIT = 50;

    private final InvoiceRepository invoices;
    private final PaymentGateway gateway;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;

    public BillingService(InvoiceRepository invoices, PaymentGateway gateway, TransactionTemplate tx,
                          ApplicationEventPublisher events) {
        this.invoices = invoices;
        this.gateway = gateway;
        this.tx = tx;
        this.events = events;
    }

    /** Charges a newly placed order. Safe to call more than once for the same order. */
    public void handleOrderPlaced(OrderPlacedEvent order) {
        if (invoices.existsForOrder(order.orderId())) {
            log.info("Order {} already billed, skipping duplicate event", order.orderId());
            return;
        }

        ChargeResult charge = gateway.charge(order.orderId(), order.userId(), order.total());
        InvoiceStatus status = charge.approved() ? InvoiceStatus.PAID : InvoiceStatus.FAILED;
        String invoiceNumber = invoiceNumber(order.orderId());

        try {
            tx.executeWithoutResult(txStatus -> {
                invoices.insert(invoiceNumber, order, status, charge.reference(), charge.failureReason());
                events.publishEvent(new PaymentCompletedEvent(order.orderId(), order.userId(), status.name(),
                        invoiceNumber, order.total(), charge.failureReason(), Instant.now()));
            });
            log.info("Order {} billed: {} {} ({})", order.orderId(), status, order.total(), invoiceNumber);
        } catch (DuplicateKeyException e) {
            log.info("Order {} was billed concurrently, skipping", order.orderId());
        }
    }

    /** Refunds a paid order that the customer or an admin cancelled. */
    public void handleOrderCancelled(OrderCancelledEvent cancelled) {
        invoices.findByOrder(cancelled.orderId()).ifPresentOrElse(invoice -> {
            if (invoice.status() != InvoiceStatus.PAID) {
                log.info("Invoice {} is {}, nothing to refund", invoice.invoiceNumber(), invoice.status());
                return;
            }
            String refundReference = gateway.refund(invoice.paymentReference(), invoice.total());
            if (invoices.markRefunded(invoice.id(), refundReference)) {
                log.info("Refunded {} for order {} ({})", invoice.total(), invoice.orderId(), refundReference);
            }
        }, () -> log.warn("Cancellation for order {} arrived before it was billed", cancelled.orderId()));
    }

    public List<Invoice> invoicesFor(AuthenticatedUser user) {
        return invoices.findByUser(user.userId(), LIST_LIMIT);
    }

    public List<Invoice> allInvoices(AuthenticatedUser user) {
        requireAdmin(user);
        return invoices.findRecent(LIST_LIMIT);
    }

    public Invoice invoiceForOrder(AuthenticatedUser user, long orderId) {
        Invoice invoice = invoices.findByOrder(orderId)
                .orElseThrow(() -> ApiException.notFound("No invoice for order " + orderId + " yet"));
        if (!user.isAdmin() && invoice.userId() != user.userId()) {
            throw ApiException.notFound("No invoice for order " + orderId + " yet");
        }
        return invoice;
    }

    public BillingSummary summary(AuthenticatedUser user) {
        requireAdmin(user);
        Map<InvoiceStatus, StatusTotals> totals = invoices.totalsByStatus();
        StatusTotals paid = totals.get(InvoiceStatus.PAID);
        StatusTotals failed = totals.get(InvoiceStatus.FAILED);
        StatusTotals refunded = totals.get(InvoiceStatus.REFUNDED);

        Instant sevenDaysAgo = LocalDate.now(BILLING_ZONE).minusDays(6).atStartOfDay(BILLING_ZONE).toInstant();
        return new BillingSummary(
                paid.count() + failed.count() + refunded.count(),
                paid.count(),
                failed.count(),
                refunded.count(),
                paid.amount().add(refunded.amount()),
                refunded.amount(),
                paid.amount(),
                invoices.dailyRevenueSince(sevenDaysAgo),
                Instant.now());
    }

    static String invoiceNumber(long orderId) {
        return "HB-" + LocalDate.now(BILLING_ZONE).format(INVOICE_DATE) + "-" + String.format("%06d", orderId);
    }

    private static void requireAdmin(AuthenticatedUser user) {
        if (!user.isAdmin()) {
            throw ApiException.forbidden("Admin role required");
        }
    }
}
