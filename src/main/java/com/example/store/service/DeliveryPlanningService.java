package com.example.store.service;

import com.example.store.domain.CustomerOrder;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.store.repository.OrderRepository;

@Service
public class DeliveryPlanningService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryPlanningService.class);

    private final OrderRepository orders;

    public DeliveryPlanningService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public void confirm(UUID orderId) {
        CustomerOrder order = orders.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order " + orderId + " not found for event"));
        if (order.confirm()) {
            log.info("Order {} scheduled into delivery slot {}", orderId, order.getSlotId());
        } else {
            log.info("Order {} already confirmed, ignoring duplicate event", orderId);
        }
    }
}
