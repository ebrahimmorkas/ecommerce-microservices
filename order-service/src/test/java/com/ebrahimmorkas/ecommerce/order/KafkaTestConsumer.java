package com.ebrahimmorkas.ecommerce.order;

import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.core.ConsumerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;

/** Test helper that reads a topic from the beginning and waits for the event with a given key. */
public final class KafkaTestConsumer implements AutoCloseable {

    private final Consumer<String, Object> consumer;

    public KafkaTestConsumer(ConsumerFactory<String, Object> consumerFactory, String... topics) {
        this.consumer = consumerFactory.createConsumer("test-" + UUID.randomUUID(), null);
        this.consumer.subscribe(List.of(topics));
    }

    public <T> T awaitEvent(String key, Class<T> type) {
        List<Object> matches = new ArrayList<>();
        await().atMost(Duration.ofSeconds(30)).until(() -> {
            consumer.poll(Duration.ofMillis(250)).forEach(record -> {
                if (key.equals(record.key()) && type.isInstance(record.value())) {
                    matches.add(record.value());
                }
            });
            return !matches.isEmpty();
        });
        return type.cast(matches.getFirst());
    }

    @Override
    public void close() {
        consumer.close();
    }
}
