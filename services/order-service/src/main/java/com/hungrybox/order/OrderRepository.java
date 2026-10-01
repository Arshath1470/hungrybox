package com.hungrybox.order;

import com.hungrybox.order.OrderDtos.OrderItemView;
import com.hungrybox.order.OrderDtos.OrderView;
import com.hungrybox.order.PricingCalculator.PriceBreakdown;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private static final String ORDER_COLUMNS = """
            id, user_id, restaurant_id, status, subtotal, delivery_fee, tax, total,
            delivery_address, invoice_number, status_reason, created_at, updated_at""";

    private final NamedParameterJdbcTemplate jdbc;

    public OrderRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(long userId, long restaurantId, PriceBreakdown price, String deliveryAddress,
                       List<OrderItemView> items) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO orders (user_id, restaurant_id, status, subtotal, delivery_fee, tax, total, delivery_address)
                VALUES (:userId, :restaurantId, :status, :subtotal, :deliveryFee, :tax, :total, :deliveryAddress)
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("restaurantId", restaurantId)
                        .addValue("status", OrderStatus.PENDING_PAYMENT.name())
                        .addValue("subtotal", price.subtotal())
                        .addValue("deliveryFee", price.deliveryFee())
                        .addValue("tax", price.tax())
                        .addValue("total", price.total())
                        .addValue("deliveryAddress", deliveryAddress),
                keys, new String[] {"id"});
        long orderId = keys.getKey().longValue();

        SqlParameterSource[] rows = items.stream()
                .map(item -> new MapSqlParameterSource()
                        .addValue("orderId", orderId)
                        .addValue("menuItemId", item.menuItemId())
                        .addValue("name", item.name())
                        .addValue("unitPrice", item.unitPrice())
                        .addValue("quantity", item.quantity())
                        .addValue("lineTotal", item.lineTotal()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate("""
                INSERT INTO order_items (order_id, menu_item_id, name, unit_price, quantity, line_total)
                VALUES (:orderId, :menuItemId, :name, :unitPrice, :quantity, :lineTotal)
                """, rows);
        return orderId;
    }

    public Optional<OrderView> findById(long id) {
        List<OrderView> orders = jdbc.query("SELECT " + ORDER_COLUMNS + " FROM orders WHERE id = :id",
                new MapSqlParameterSource("id", id), OrderRepository::mapOrder);
        return withItems(orders).stream().findFirst();
    }

    public List<OrderView> findByUser(long userId, int limit) {
        List<OrderView> orders = jdbc.query(
                "SELECT " + ORDER_COLUMNS + " FROM orders WHERE user_id = :userId ORDER BY id DESC LIMIT " + limit,
                new MapSqlParameterSource("userId", userId), OrderRepository::mapOrder);
        return withItems(orders);
    }

    public List<OrderView> findRecent(int limit) {
        List<OrderView> orders = jdbc.query(
                "SELECT " + ORDER_COLUMNS + " FROM orders ORDER BY id DESC LIMIT " + limit,
                new MapSqlParameterSource(), OrderRepository::mapOrder);
        return withItems(orders);
    }

    /**
     * Compare-and-set status change. Returns false if another writer moved the order first,
     * which keeps duplicate Kafka deliveries and concurrent admin clicks harmless.
     */
    public boolean transition(long id, OrderStatus expected, OrderStatus next, String reason, String invoiceNumber) {
        int updated = jdbc.update("""
                UPDATE orders
                   SET status = :next,
                       status_reason = COALESCE(:reason, status_reason),
                       invoice_number = COALESCE(:invoiceNumber, invoice_number),
                       updated_at = CURRENT_TIMESTAMP
                 WHERE id = :id AND status = :expected
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("expected", expected.name())
                        .addValue("next", next.name())
                        .addValue("reason", reason, java.sql.Types.VARCHAR)
                        .addValue("invoiceNumber", invoiceNumber, java.sql.Types.VARCHAR));
        return updated == 1;
    }

    private List<OrderView> withItems(List<OrderView> orders) {
        if (orders.isEmpty()) {
            return orders;
        }
        List<Long> ids = orders.stream().map(OrderView::id).toList();
        Map<Long, List<OrderItemView>> itemsByOrder = new LinkedHashMap<>();
        jdbc.query("""
                SELECT order_id, menu_item_id, name, unit_price, quantity, line_total
                  FROM order_items WHERE order_id IN (:ids) ORDER BY id
                """,
                new MapSqlParameterSource("ids", ids),
                (ResultSet rs) -> {
                    itemsByOrder.computeIfAbsent(rs.getLong("order_id"), k -> new ArrayList<>())
                            .add(new OrderItemView(
                                    rs.getLong("menu_item_id"),
                                    rs.getString("name"),
                                    rs.getBigDecimal("unit_price"),
                                    rs.getInt("quantity"),
                                    rs.getBigDecimal("line_total")));
                });
        return orders.stream()
                .map(order -> withItems(order, itemsByOrder.getOrDefault(order.id(), List.of())))
                .toList();
    }

    private static OrderView withItems(OrderView o, List<OrderItemView> items) {
        return new OrderView(o.id(), o.userId(), o.restaurantId(), o.status(), o.subtotal(), o.deliveryFee(),
                o.tax(), o.total(), o.deliveryAddress(), o.invoiceNumber(), o.statusReason(), o.createdAt(),
                o.updatedAt(), items);
    }

    private static OrderView mapOrder(ResultSet rs, int rowNum) throws SQLException {
        return new OrderView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("restaurant_id"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("delivery_fee"),
                rs.getBigDecimal("tax"),
                rs.getBigDecimal("total"),
                rs.getString("delivery_address"),
                rs.getString("invoice_number"),
                rs.getString("status_reason"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")),
                List.of());
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
