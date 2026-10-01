package com.hungrybox.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record PlaceOrderRequest(
            @NotNull Long restaurantId,
            @NotEmpty @Size(max = 30) List<@Valid ItemRequest> items,
            @NotBlank @Size(max = 255) String deliveryAddress) {
    }

    public record ItemRequest(@NotNull Long menuItemId, @Min(1) @Max(20) int quantity) {
    }

    public record StatusUpdateRequest(@NotNull OrderStatus status) {
    }

    public record OrderItemView(long menuItemId, String name, BigDecimal unitPrice, int quantity,
                                BigDecimal lineTotal) {
    }

    public record OrderView(
            long id,
            long userId,
            long restaurantId,
            OrderStatus status,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal tax,
            BigDecimal total,
            String deliveryAddress,
            String invoiceNumber,
            String statusReason,
            Instant createdAt,
            Instant updatedAt,
            List<OrderItemView> items) {
    }

    /** Menu item as returned by restaurant-service's lookup endpoint. */
    public record MenuItemSnapshot(long id, long restaurantId, String name, BigDecimal price, boolean available) {
    }
}
