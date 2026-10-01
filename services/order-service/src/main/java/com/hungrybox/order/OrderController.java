package com.hungrybox.order;

import com.hungrybox.common.security.AuthenticatedUser;
import com.hungrybox.common.security.CurrentUser;
import com.hungrybox.order.OrderDtos.OrderView;
import com.hungrybox.order.OrderDtos.PlaceOrderRequest;
import com.hungrybox.order.OrderDtos.StatusUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@CurrentUser AuthenticatedUser user, @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(user, request);
    }

    @GetMapping
    public List<OrderView> mine(@CurrentUser AuthenticatedUser user) {
        return orderService.listMine(user);
    }

    @GetMapping("/all")
    public List<OrderView> all(@CurrentUser AuthenticatedUser user) {
        return orderService.listAll(user);
    }

    @GetMapping("/{id}")
    public OrderView get(@CurrentUser AuthenticatedUser user, @PathVariable long id) {
        return orderService.get(user, id);
    }

    @PostMapping("/{id}/cancel")
    public OrderView cancel(@CurrentUser AuthenticatedUser user, @PathVariable long id) {
        return orderService.cancel(user, id);
    }

    @PatchMapping("/{id}/status")
    public OrderView updateStatus(@CurrentUser AuthenticatedUser user, @PathVariable long id,
                                  @Valid @RequestBody StatusUpdateRequest request) {
        return orderService.updateStatus(user, id, request.status());
    }
}
