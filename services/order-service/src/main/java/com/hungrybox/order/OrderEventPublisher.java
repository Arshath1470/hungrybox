package com.hungrybox.order;

import com.hungrybox.common.events.EventJson;
import com.hungrybox.common.events.OrderCancelledEvent;
import com.hungrybox.common.events.OrderPlacedEvent;
import com.hungrybox.common.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges committed database changes to Kafka. Because events are only sent after
 * the order row commits, consumers never see an order that was rolled back.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final EventJson json;

    public OrderEventPublisher(KafkaTemplate<String, String> kafka, EventJson json) {
        this.kafka = kafka;
        this.json = json;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        send(Topics.ORDER_PLACED, event.orderId(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        send(Topics.ORDER_CANCELLED, event.orderId(), event);
    }

    private void send(String topic, long orderId, Object event) {
        kafka.send(topic, String.valueOf(orderId), json.write(event)).whenComplete((result, error) -> {
            if (error != null) {
                log.error("Failed to publish {} for order {}", topic, orderId, error);
            } else {
                log.info("Published {} for order {}", topic, orderId);
            }
        });
    }
}
