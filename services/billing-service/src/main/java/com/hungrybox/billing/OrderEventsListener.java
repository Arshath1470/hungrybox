package com.hungrybox.billing;

import com.hungrybox.common.events.EventJson;
import com.hungrybox.common.events.OrderCancelledEvent;
import com.hungrybox.common.events.OrderPlacedEvent;
import com.hungrybox.common.events.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsListener {

    private final BillingService billingService;
    private final EventJson json;

    public OrderEventsListener(BillingService billingService, EventJson json) {
        this.billingService = billingService;
        this.json = json;
    }

    @KafkaListener(topics = Topics.ORDER_PLACED, groupId = "${spring.application.name}",
            autoStartup = "${hungrybox.kafka.listeners-enabled:true}")
    public void onOrderPlaced(String payload) {
        billingService.handleOrderPlaced(json.read(payload, OrderPlacedEvent.class));
    }

    @KafkaListener(topics = Topics.ORDER_CANCELLED, groupId = "${spring.application.name}",
            autoStartup = "${hungrybox.kafka.listeners-enabled:true}")
    public void onOrderCancelled(String payload) {
        billingService.handleOrderCancelled(json.read(payload, OrderCancelledEvent.class));
    }
}
