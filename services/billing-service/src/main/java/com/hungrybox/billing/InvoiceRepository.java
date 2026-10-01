package com.hungrybox.billing;

import com.hungrybox.billing.BillingDtos.DailyRevenue;
import com.hungrybox.billing.BillingDtos.Invoice;
import com.hungrybox.billing.BillingDtos.InvoiceStatus;
import com.hungrybox.common.events.OrderPlacedEvent;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceRepository {

    private static final String COLUMNS = """
            id, invoice_number, order_id, user_id, restaurant_id, subtotal, delivery_fee, tax, total, status,
            payment_reference, refund_reference, failure_reason, created_at, updated_at""";

    private final NamedParameterJdbcTemplate jdbc;

    public InvoiceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(String invoiceNumber, OrderPlacedEvent order, InvoiceStatus status, String paymentReference,
                       String failureReason) {
        jdbc.update("""
                INSERT INTO invoices (invoice_number, order_id, user_id, restaurant_id, subtotal, delivery_fee, tax,
                                      total, status, payment_reference, failure_reason)
                VALUES (:invoiceNumber, :orderId, :userId, :restaurantId, :subtotal, :deliveryFee, :tax,
                        :total, :status, :paymentReference, :failureReason)
                """,
                new MapSqlParameterSource()
                        .addValue("invoiceNumber", invoiceNumber)
                        .addValue("orderId", order.orderId())
                        .addValue("userId", order.userId())
                        .addValue("restaurantId", order.restaurantId())
                        .addValue("subtotal", order.subtotal())
                        .addValue("deliveryFee", order.deliveryFee())
                        .addValue("tax", order.tax())
                        .addValue("total", order.total())
                        .addValue("status", status.name())
                        .addValue("paymentReference", paymentReference, java.sql.Types.VARCHAR)
                        .addValue("failureReason", failureReason, java.sql.Types.VARCHAR));
    }

    public boolean existsForOrder(long orderId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM invoices WHERE order_id = :orderId",
                new MapSqlParameterSource("orderId", orderId), Integer.class);
        return count != null && count > 0;
    }

    public Optional<Invoice> findByOrder(long orderId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM invoices WHERE order_id = :orderId",
                new MapSqlParameterSource("orderId", orderId), InvoiceRepository::map).stream().findFirst();
    }

    public List<Invoice> findByUser(long userId, int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM invoices WHERE user_id = :userId ORDER BY id DESC LIMIT " + limit,
                new MapSqlParameterSource("userId", userId), InvoiceRepository::map);
    }

    public List<Invoice> findRecent(int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM invoices ORDER BY id DESC LIMIT " + limit,
                new MapSqlParameterSource(), InvoiceRepository::map);
    }

    public boolean markRefunded(long invoiceId, String refundReference) {
        return jdbc.update("""
                UPDATE invoices SET status = 'REFUNDED', refund_reference = :ref, updated_at = CURRENT_TIMESTAMP
                 WHERE id = :id AND status = 'PAID'
                """,
                new MapSqlParameterSource().addValue("id", invoiceId).addValue("ref", refundReference)) == 1;
    }

    public Map<InvoiceStatus, StatusTotals> totalsByStatus() {
        Map<InvoiceStatus, StatusTotals> totals = new EnumMap<>(InvoiceStatus.class);
        for (InvoiceStatus status : InvoiceStatus.values()) {
            totals.put(status, new StatusTotals(0, BigDecimal.ZERO));
        }
        jdbc.query("SELECT status, COUNT(*) AS cnt, COALESCE(SUM(total), 0) AS amount FROM invoices GROUP BY status",
                new MapSqlParameterSource(),
                (ResultSet rs) -> {
                    totals.put(InvoiceStatus.valueOf(rs.getString("status")),
                            new StatusTotals(rs.getLong("cnt"), rs.getBigDecimal("amount")));
                });
        return totals;
    }

    public List<DailyRevenue> dailyRevenueSince(Instant since) {
        return jdbc.query("""
                SELECT CAST(created_at AS DATE) AS sale_date, COUNT(*) AS cnt, SUM(total) AS amount
                  FROM invoices
                 WHERE status = 'PAID' AND created_at >= :since
                 GROUP BY CAST(created_at AS DATE)
                 ORDER BY sale_date
                """,
                new MapSqlParameterSource("since", Timestamp.from(since)),
                (rs, rowNum) -> new DailyRevenue(rs.getDate("sale_date").toLocalDate(), rs.getLong("cnt"),
                        rs.getBigDecimal("amount")));
    }

    public record StatusTotals(long count, BigDecimal amount) {
    }

    private static Invoice map(ResultSet rs, int rowNum) throws SQLException {
        return new Invoice(
                rs.getLong("id"),
                rs.getString("invoice_number"),
                rs.getLong("order_id"),
                rs.getLong("user_id"),
                rs.getLong("restaurant_id"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("delivery_fee"),
                rs.getBigDecimal("tax"),
                rs.getBigDecimal("total"),
                InvoiceStatus.valueOf(rs.getString("status")),
                rs.getString("payment_reference"),
                rs.getString("refund_reference"),
                rs.getString("failure_reason"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
