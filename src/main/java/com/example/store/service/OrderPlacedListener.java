package com.example.store.service;

import com.example.store.domain.OrderPlacedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedListener {

    private final ObjectMapper mapper;
    private final DeliveryPlanningService planning;

    public OrderPlacedListener(ObjectMapper mapper, DeliveryPlanningService planning) {
        this.mapper = mapper;
        this.planning = planning;
    }

    @KafkaListener(topics = "${store.topics.order-placed}", groupId = "delivery-planner")
    public void onOrderPlaced(String payload) throws JsonProcessingException {
        OrderPlacedEvent event = mapper.readValue(payload, OrderPlacedEvent.class);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("orderId", event.orderId().toString())) {
            planning.confirm(event.orderId());
        }
    }
}
