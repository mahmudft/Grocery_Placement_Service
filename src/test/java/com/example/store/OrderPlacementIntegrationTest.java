package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.store.domain.DeliverySlot;
import com.example.store.domain.OrderStatus;
import com.example.store.domain.Product;
import com.example.store.domain.StoreException;
import com.example.store.repository.DeliverySlotRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.service.OrderService;
import com.example.store.web.Api;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderPlacementIntegrationTest extends AbstractIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired ProductRepository products;
    @Autowired DeliverySlotRepository slots;

    @Test
    void neverOversellsUnderConcurrentCheckouts() throws Exception {
        String sku = newProduct(10);
        DeliverySlot slot = newSlot(1000);

        Outcome outcome = runConcurrently(40, i -> orderService.place(request("customer-" + i, slot.getId(), sku, 1), key()));

        assertThat(outcome.accepted()).isEqualTo(10);
        assertThat(outcome.rejected()).isEqualTo(30);
        assertThat(products.findById(sku).orElseThrow().getStock()).isZero();
    }

    @Test
    void neverOverbooksSlotAndRollsBackStockOfRejectedOrders() throws Exception {
        String sku = newProduct(100);
        DeliverySlot slot = newSlot(3);

        Outcome outcome = runConcurrently(20, i -> orderService.place(request("customer-" + i, slot.getId(), sku, 2), key()));

        assertThat(outcome.accepted()).isEqualTo(3);
        assertThat(slots.findById(slot.getId()).orElseThrow().getBooked()).isEqualTo(3);
        // Rejected orders reserved stock before hitting the full slot; the rollback must have given it back.
        assertThat(products.findById(sku).orElseThrow().getStock()).isEqualTo(100 - 3 * 2);
    }

    @Test
    void sameIdempotencyKeyCreatesOneOrderAndDecrementsStockOnce() {
        String sku = newProduct(10);
        DeliverySlot slot = newSlot(10);
        String key = key();

        OrderService.Result first = orderService.place(request("alice", slot.getId(), sku, 3), key);
        OrderService.Result replay = orderService.place(request("alice", slot.getId(), sku, 3), key);

        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(replay.order().id()).isEqualTo(first.order().id());
        assertThat(products.findById(sku).orElseThrow().getStock()).isEqualTo(7);
    }

    @Test
    void placedOrderIsConfirmedAsynchronouslyViaOutboxAndKafka() {
        String sku = newProduct(10);
        DeliverySlot slot = newSlot(10);

        UUID orderId = orderService.place(request("bob", slot.getId(), sku, 1), key()).order().id();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(orderService.get(orderId).status()).isEqualTo(OrderStatus.CONFIRMED));
    }

    // ---------- helpers ----------

    private String newProduct(int stock) {
        String sku = "TEST-" + UUID.randomUUID();
        products.save(new Product(sku, "Test product", 100, stock));
        return sku;
    }

    private DeliverySlot newSlot(int capacity) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        return slots.save(new DeliverySlot(start, start.plus(2, ChronoUnit.HOURS), capacity));
    }

    private static Api.PlaceOrderRequest request(String customer, Long slotId, String sku, int qty) {
        return new Api.PlaceOrderRequest(customer, slotId, List.of(new Api.Item(sku, qty)));
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private interface Attempt { void run(int i) throws Exception; }

    private record Outcome(int accepted, int rejected) {}

    /** Fires all attempts at the same instant from separate threads; business rejections are counted, anything else fails the test. */
    private Outcome runConcurrently(int threads, Attempt attempt) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch gate = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int n = i;
            Callable<Void> task = () -> {
                gate.await();
                try {
                    attempt.run(n);
                    accepted.incrementAndGet();
                } catch (StoreException e) {
                    rejected.incrementAndGet();
                }
                return null;
            };
            futures.add(pool.submit(task));
        }
        gate.countDown();
        for (Future<?> f : futures) {
            f.get(60, java.util.concurrent.TimeUnit.SECONDS);
        }
        pool.shutdownNow();
        return new Outcome(accepted.get(), rejected.get());
    }
}
