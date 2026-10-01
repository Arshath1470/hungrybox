package com.hungrybox.order;

import com.hungrybox.common.events.OrderCancelledEvent;
import com.hungrybox.common.events.OrderPlacedEvent;
import com.hungrybox.common.events.PaymentCompletedEvent;
import com.hungrybox.common.security.AuthenticatedUser;
import com.hungrybox.common.web.ApiException;
import com.hungrybox.order.OrderDtos.ItemRequest;
import com.hungrybox.order.OrderDtos.MenuItemSnapshot;
import com.hungrybox.order.OrderDtos.OrderItemView;
import com.hungrybox.order.OrderDtos.OrderView;
import com.hungrybox.order.OrderDtos.PlaceOrderRequest;
import com.hungrybox.order.PricingCalculator.Line;
import com.hungrybox.order.PricingCalculator.PriceBreakdown;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    static final int MAX_QUANTITY_PER_ITEM = 20;
    private static final int LIST_LIMIT = 50;

    /** Admins drive the kitchen/delivery steps; payment outcomes only ever come from billing-service. */
    private static final Set<OrderStatus> ADMIN_SETTABLE =
            EnumSet.of(OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.CANCELLED);

    private final OrderRepository orders;
    private final RestaurantClient restaurantClient;
    private final PricingCalculator pricing;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, RestaurantClient restaurantClient, PricingCalculator pricing,
                        TransactionTemplate tx, ApplicationEventPublisher events) {
        this.orders = orders;
        this.restaurantClient = restaurantClient;
        this.pricing = pricing;
        this.tx = tx;
        this.events = events;
    }

    public OrderView placeOrder(AuthenticatedUser user, PlaceOrderRequest request) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (ItemRequest item : request.items()) {
            quantities.merge(item.menuItemId(), item.quantity(), Integer::sum);
        }
        quantities.forEach((id, qty) -> {
            if (qty > MAX_QUANTITY_PER_ITEM) {
                throw ApiException.badRequest("At most " + MAX_QUANTITY_PER_ITEM + " of each item per order");
            }
        });

        // Prices come from restaurant-service, never from the client.
        Map<Long, MenuItemSnapshot> menu = restaurantClient.menuItems(quantities.keySet()).stream()
                .collect(Collectors.toMap(MenuItemSnapshot::id, Function.identity()));

        List<OrderItemView> lines = new ArrayList<>();
        List<Line> pricingLines = new ArrayList<>();
        quantities.forEach((menuItemId, quantity) -> {
            MenuItemSnapshot item = menu.get(menuItemId);
            if (item == null) {
                throw ApiException.badRequest("Menu item " + menuItemId + " does not exist");
            }
            if (item.restaurantId() != request.restaurantId()) {
                throw ApiException.badRequest("All items in an order must come from the same restaurant");
            }
            if (!item.available()) {
                throw ApiException.conflict(item.name() + " is currently unavailable");
            }
            Line line = new Line(item.price(), quantity);
            pricingLines.add(line);
            lines.add(new OrderItemView(item.id(), item.name(), item.price(), quantity, line.lineTotal()));
        });

        PriceBreakdown price = pricing.price(pricingLines);
        String address = request.deliveryAddress().trim();

        long orderId = tx.execute(status -> {
            long id = orders.insert(user.userId(), request.restaurantId(), price, address, lines);
            // Delivered to Kafka only after the transaction commits (see OrderEventPublisher).
            events.publishEvent(new OrderPlacedEvent(id, user.userId(), request.restaurantId(), price.subtotal(),
                    price.deliveryFee(), price.tax(), price.total(), Instant.now()));
            return id;
        });
        log.info("Order {} placed by user {} for {}", orderId, user.userId(), price.total());
        return get(user, orderId);
    }

    public OrderView get(AuthenticatedUser user, long orderId) {
        OrderView order = orders.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order " + orderId + " not found"));
        if (!user.isAdmin() && order.userId() != user.userId()) {
            // Do not reveal that someone else's order exists.
            throw ApiException.notFound("Order " + orderId + " not found");
        }
        return order;
    }

    public List<OrderView> listMine(AuthenticatedUser user) {
        return orders.findByUser(user.userId(), LIST_LIMIT);
    }

    public List<OrderView> listAll(AuthenticatedUser user) {
        requireAdmin(user);
        return orders.findRecent(LIST_LIMIT);
    }

    public OrderView cancel(AuthenticatedUser user, long orderId) {
        OrderView order = get(user, orderId);
        if (!order.status().isCancellableByCustomer()) {
            throw ApiException.conflict("Order can no longer be cancelled (status " + order.status() + ")");
        }
        moveTo(order, OrderStatus.CANCELLED, user.isAdmin() ? "Cancelled by admin" : "Cancelled by customer");
        return get(user, orderId);
    }

    public OrderView updateStatus(AuthenticatedUser user, long orderId, OrderStatus next) {
        requireAdmin(user);
        if (!ADMIN_SETTABLE.contains(next)) {
            throw ApiException.badRequest("Status " + next + " is set by the payment flow, not manually");
        }
        OrderView order = get(user, orderId);
        if (!order.status().canTransitionTo(next)) {
            throw ApiException.conflict("Cannot move order from " + order.status() + " to " + next);
        }
        moveTo(order, next, next == OrderStatus.CANCELLED ? "Cancelled by admin" : null);
        return get(user, orderId);
    }

    /** Called for every payment.completed event. Idempotent: replays are ignored. */
    public void applyPaymentResult(PaymentCompletedEvent event) {
        OrderStatus next = event.isPaid() ? OrderStatus.CONFIRMED : OrderStatus.PAYMENT_FAILED;
        boolean changed = Boolean.TRUE.equals(tx.execute(status -> orders.transition(event.orderId(),
                OrderStatus.PENDING_PAYMENT, next, event.failureReason(), event.invoiceNumber())));
        if (changed) {
            log.info("Order {} -> {} (invoice {})", event.orderId(), next, event.invoiceNumber());
        } else {
            log.info("Ignoring payment event for order {}: not awaiting payment", event.orderId());
        }
    }

    private void moveTo(OrderView order, OrderStatus next, String reason) {
        tx.executeWithoutResult(status -> {
            if (!orders.transition(order.id(), order.status(), next, reason, null)) {
                throw ApiException.conflict("Order " + order.id() + " was updated concurrently, please retry");
            }
            if (next == OrderStatus.CANCELLED) {
                events.publishEvent(new OrderCancelledEvent(order.id(), order.userId(), reason, Instant.now()));
            }
        });
    }

    private static void requireAdmin(AuthenticatedUser user) {
        if (!user.isAdmin()) {
            throw ApiException.forbidden("Admin role required");
        }
    }
}
