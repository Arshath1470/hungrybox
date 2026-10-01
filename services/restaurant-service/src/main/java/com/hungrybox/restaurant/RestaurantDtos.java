package com.hungrybox.restaurant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class RestaurantDtos {

    private RestaurantDtos() {
    }

    public record Restaurant(long id, String name, String cuisine, String city, BigDecimal rating,
                             int deliveryTimeMins, boolean open) {
    }

    public record MenuItem(long id, long restaurantId, String name, String description, String category,
                           BigDecimal price, boolean veg, boolean available) {
    }

    public record RestaurantDetail(Restaurant restaurant, List<MenuItem> menu) {
    }

    public record CreateRestaurantRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 60) String cuisine,
            @NotBlank @Size(max = 60) String city,
            @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal rating,
            @Min(5) @Max(180) Integer deliveryTimeMins) {
    }

    public record CreateMenuItemRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 255) String description,
            @NotBlank @Size(max = 60) String category,
            @NotNull @DecimalMin("1.00") BigDecimal price,
            Boolean veg) {
    }

    public record AvailabilityRequest(@NotNull Boolean available) {
    }
}
