package com.hungrybox.order;

import com.hungrybox.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /** Retry a failing record 3 times, then park it on <topic>.DLT instead of blocking the partition. */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        DefaultErrorHandler handler =
                new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1_000L, 3L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }

    @Configuration
    @ConditionalOnProperty(name = "hungrybox.kafka.create-topics", havingValue = "true", matchIfMissing = true)
    static class TopicsConfig {

        @Bean
        NewTopic orderPlacedTopic() {
            return TopicBuilder.name(Topics.ORDER_PLACED).partitions(3).replicas(1).build();
        }

        @Bean
        NewTopic orderCancelledTopic() {
            return TopicBuilder.name(Topics.ORDER_CANCELLED).partitions(3).replicas(1).build();
        }
    }
}
