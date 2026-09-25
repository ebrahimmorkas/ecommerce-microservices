package com.ebrahimmorkas.ecommerce.inventory.config;

import com.ebrahimmorkas.ecommerce.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class KafkaConfig {

    @Bean
    NewTopic inventoryReservedTopic() {
        return TopicBuilder.name(Topics.INVENTORY_RESERVED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic inventoryReservationFailedTopic() {
        return TopicBuilder.name(Topics.INVENTORY_RESERVATION_FAILED).partitions(3).replicas(1).build();
    }

    /** Retries a failed record 3 times (1s apart), then parks it on {@code <topic>-dlt} for inspection. */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1_000L, 3));
    }
}
