package com.example.store.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic orderPlacedTopic(StoreProperties props) {
        return TopicBuilder.name(props.topics().orderPlaced()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderPlacedDeadLetterTopic(StoreProperties props) {
        // Same partition count as the source topic: default DLT routing keeps the partition number.
        return TopicBuilder.name(props.topics().orderPlaced() + ".DLT").partitions(3).replicas(1).build();
    }

    /** Retry a failing record 3 times, then park it on the dead-letter topic instead of blocking the partition. */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(500L, 3L));
    }
}
