package com.ebrahimmorkas.ecommerce.order.config;

import com.ebrahimmorkas.ecommerce.events.Topics;
import com.ebrahimmorkas.ecommerce.order.exception.OrderNotFoundException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class KafkaConfig {

    /** Declares every saga topic up front (idempotent), before any listener subscribes. */
    @Bean
    KafkaAdmin.NewTopics sagaTopics() {
        return new KafkaAdmin.NewTopics(Topics.ALL.stream()
                .map(topic -> TopicBuilder.name(topic).partitions(Topics.PARTITIONS).replicas(1).build())
                .toArray(NewTopic[]::new));
    }

    /**
     * Retries a failed record 3 times (1s apart), then parks it on {@code <topic>-dlt}. Errors that
     * can never succeed on retry (unknown order, illegal state transition) go straight to the DLT.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1_000L, 3));
        handler.addNotRetryableExceptions(OrderNotFoundException.class, IllegalStateException.class);
        return handler;
    }
}
