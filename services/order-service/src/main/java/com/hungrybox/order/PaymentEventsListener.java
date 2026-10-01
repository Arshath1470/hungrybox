package com.hungrybox.order;

import com.hungrybox.common.events.EventJson;
import com.hungrybox.common.events.PaymentCompletedEvent;
import com.hungrybox.common.events.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventsListener {

    private final OrderService orderService;
    private final EventJson json;

    public PaymentEventsListener(OrderService orderService, EventJson json) {
        this.orderService = orderService;
        this.json = json;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "${spring.application.name}",
            autoStartup = "${hungrybox.kafka.listeners-enabled:true}")
    public void onPaymentCompleted(String payload) {
        orderService.applyPaymentResult(json.read(payload, PaymentCompletedEvent.class));
    }
}
