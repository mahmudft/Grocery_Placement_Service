package com.example.store.service;

import com.example.store.domain.CustomerOrder;
import com.example.store.domain.DeliverySlot;
import com.example.store.domain.OrderPlacedEvent;
import com.example.store.domain.OutboxEvent;
import com.example.store.domain.Product;
import com.example.store.domain.StoreException;
import com.example.store.repository.DeliverySlotRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OutboxRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.web.Api;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public record Result(Api.OrderResponse order, boolean created) {}

    private final OrderRepository orders;
    private final ProductRepository products;
    private final DeliverySlotRepository slots;
    private final OutboxRepository outbox;
    private final SlotHoldService holds;
    private final ObjectMapper mapper;
    private final Clock clock;

    public OrderService(OrderRepository orders, ProductRepository products, DeliverySlotRepository slots,
                        OutboxRepository outbox, SlotHoldService holds, ObjectMapper mapper, Clock clock) {
        this.orders = orders;
        this.products = products;
        this.slots = slots;
        this.outbox = outbox;
        this.holds = holds;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public Result place(Api.PlaceOrderRequest request, String idempotencyKey) {
        var replay = orders.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) {
            log.info("Idempotent replay for key {}", idempotencyKey);
            return new Result(Api.OrderResponse.from(replay.get()), false);
        }

        DeliverySlot slot = slots.findById(request.slotId()).orElseThrow(
                () -> StoreException.notFound("Delivery slot %d not found".formatted(request.slotId())));
        if (!slot.getStartsAt().isAfter(clock.instant())) {
            throw StoreException.conflict("Delivery slot %d has already started".formatted(slot.getId()));
        }

        Map<String, Integer> quantities = new TreeMap<>();
        request.items().forEach(i -> quantities.merge(i.sku(), i.quantity(), Integer::sum));

        CustomerOrder order = new CustomerOrder(request.customerId(), slot.getId(), idempotencyKey, clock.instant());
        for (var entry : quantities.entrySet()) {
            String sku = entry.getKey();
            int qty = entry.getValue();
            Product product = products.findById(sku)
                    .orElseThrow(() -> StoreException.notFound("Unknown product " + sku));
            if (products.decrementStock(sku, qty) == 0) {
                throw StoreException.conflict("Insufficient stock for " + sku);
            }
            order.addLine(sku, qty, product.getPriceCents());
        }

        if (slots.reserveCapacity(slot.getId()) == 0) {
            throw StoreException.conflict("Delivery slot %d is fully booked".formatted(slot.getId()));
        }

        order = orders.save(order);
        outbox.save(new OutboxEvent(order.getId().toString(), "OrderPlaced", toJson(eventFor(order)), clock.instant()));
        releaseHoldAfterCommit(slot.getId(), request.customerId());

        log.info("Order {} placed for customer {} (total {} cents)", order.getId(), order.getCustomerId(), order.getTotalCents());
        return new Result(Api.OrderResponse.from(order), true);
    }

    @Transactional(readOnly = true)
    public Api.OrderResponse get(UUID id) {
        return orders.findById(id).map(Api.OrderResponse::from)
                .orElseThrow(() -> StoreException.notFound("Order %s not found".formatted(id)));
    }

    private OrderPlacedEvent eventFor(CustomerOrder o) {
        List<OrderPlacedEvent.Line> lines = o.getLines().stream()
                .map(l -> new OrderPlacedEvent.Line(l.getSku(), l.getQuantity())).toList();
        return new OrderPlacedEvent(o.getId(), o.getCustomerId(), o.getSlotId(), o.getTotalCents(), o.getCreatedAt(), lines);
    }

    private String toJson(OrderPlacedEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise event", e);
        }
    }

    /** The Redis hold is only released once the DB commit succeeded; a Redis hiccup must not fail the order. */
    private void releaseHoldAfterCommit(long slotId, String customerId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    holds.release(slotId, customerId);
                } catch (RuntimeException e) {
                    log.warn("Could not release slot hold for slot {} / customer {}; it will expire by TTL", slotId, customerId, e);
                }
            }
        });
    }
}
