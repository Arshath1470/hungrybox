package com.hungrybox.restaurant;

import com.hungrybox.common.security.AuthenticatedUser;
import com.hungrybox.common.security.CurrentUser;
import com.hungrybox.common.web.ApiException;
import com.hungrybox.restaurant.RestaurantDtos.AvailabilityRequest;
import com.hungrybox.restaurant.RestaurantDtos.CreateMenuItemRequest;
import com.hungrybox.restaurant.RestaurantDtos.CreateRestaurantRequest;
import com.hungrybox.restaurant.RestaurantDtos.MenuItem;
import com.hungrybox.restaurant.RestaurantDtos.Restaurant;
import com.hungrybox.restaurant.RestaurantDtos.RestaurantDetail;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private static final int MAX_LOOKUP_IDS = 100;

    private final RestaurantRepository repository;

    public RestaurantController(RestaurantRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Restaurant> list(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) String cuisine,
                                 @RequestParam(required = false) String city) {
        return repository.search(q, cuisine, city);
    }

    @GetMapping("/cuisines")
    public List<String> cuisines() {
        return repository.cuisines();
    }

    @GetMapping("/{id}")
    public RestaurantDetail detail(@PathVariable long id) {
        Restaurant restaurant = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Restaurant " + id + " not found"));
        return new RestaurantDetail(restaurant, repository.menuFor(id));
    }

    /** Used by order-service to price an order server-side, so clients can never set their own prices. */
    @GetMapping("/menu-items")
    public List<MenuItem> menuItems(@RequestParam List<Long> ids) {
        Set<Long> unique = new LinkedHashSet<>(ids);
        if (unique.size() > MAX_LOOKUP_IDS) {
            throw ApiException.badRequest("At most " + MAX_LOOKUP_IDS + " ids per lookup");
        }
        return repository.menuItemsByIds(unique);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Restaurant create(@CurrentUser AuthenticatedUser user, @Valid @RequestBody CreateRestaurantRequest request) {
        requireAdmin(user);
        BigDecimal rating = request.rating() == null ? new BigDecimal("4.0") : request.rating();
        int deliveryTime = request.deliveryTimeMins() == null ? 30 : request.deliveryTimeMins();
        long id = repository.insertRestaurant(request.name().trim(), request.cuisine().trim(), request.city().trim(),
                rating, deliveryTime);
        return repository.findById(id).orElseThrow();
    }

    @PostMapping("/{id}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItem addMenuItem(@CurrentUser AuthenticatedUser user, @PathVariable long id,
                                @Valid @RequestBody CreateMenuItemRequest request) {
        requireAdmin(user);
        repository.findById(id).orElseThrow(() -> ApiException.notFound("Restaurant " + id + " not found"));
        long itemId = repository.insertMenuItem(id, request.name().trim(), request.description(),
                request.category().trim(), request.price(), request.veg() == null || request.veg());
        return repository.findMenuItem(itemId).orElseThrow();
    }

    @PatchMapping("/menu-items/{itemId}/availability")
    public MenuItem setAvailability(@CurrentUser AuthenticatedUser user, @PathVariable long itemId,
                                    @Valid @RequestBody AvailabilityRequest request) {
        requireAdmin(user);
        if (repository.setAvailability(itemId, request.available()) == 0) {
            throw ApiException.notFound("Menu item " + itemId + " not found");
        }
        return repository.findMenuItem(itemId).orElseThrow();
    }

    private static void requireAdmin(AuthenticatedUser user) {
        if (!user.isAdmin()) {
            throw ApiException.forbidden("Admin role required");
        }
    }
}
