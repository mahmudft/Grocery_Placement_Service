package com.example.store.service;

import com.example.store.config.StoreProperties;
import com.example.store.domain.OutboxEvent;
import com.example.store.repository.OutboxRepository;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final Clock clock;
    private final String topic;

    public OutboxPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka, Clock clock, StoreProperties props) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.clock = clock;
        this.topic = props.topics().orderPlaced();
    }

    @Scheduled(fixedDelayString = "${store.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outbox.lockNextBatch(PageRequest.of(0, 100));
        for (OutboxEvent event : batch) {
            try {
                // Keyed by order id: all events of one order land on one partition, in order.
                kafka.send(topic, event.getAggregateId(), event.getPayload()).get(5, TimeUnit.SECONDS);
                event.markPublished(clock.instant());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException e) {
                log.warn("Kafka publish failed for outbox event of aggregate {}; will retry", event.getAggregateId(), e);
                return; // rows already marked are committed; this one stays unpublished and is retried
            }
        }
    }
}
