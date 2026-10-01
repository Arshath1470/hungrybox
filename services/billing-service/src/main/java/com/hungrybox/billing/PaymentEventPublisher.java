package com.hungrybox.billing;

import com.hungrybox.common.events.EventJson;
import com.hungrybox.common.events.PaymentCompletedEvent;
import com.hungrybox.common.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Publishes payment outcomes only after the invoice row has committed. */
@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;
    private final EventJson json;

    public PaymentEventPublisher(KafkaTemplate<String, String> kafka, EventJson json) {
        this.kafka = kafka;
        this.json = json;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        String key = String.valueOf(event.orderId());
        kafka.send(Topics.PAYMENT_COMPLETED, key, json.write(event)).whenComplete((result, error) -> {
            if (error != null) {
                log.error("Failed to publish payment result for order {}", event.orderId(), error);
            } else {
                log.info("Published payment {} for order {}", event.status(), event.orderId());
            }
        });
    }
}
