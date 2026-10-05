package com.example.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.store.domain.DeliverySlot;
import com.example.store.domain.Product;
import com.example.store.repository.DeliverySlotRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.web.Api;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class OrderApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate http;
    @Autowired ProductRepository products;
    @Autowired DeliverySlotRepository slots;

    @Test
    void healthEndpointIsUp() {
        ResponseEntity<String> response = http.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void placesOrderAndReturns201WithLocation() {
        String sku = sku(5);
        long slotId = slot(5);

        ResponseEntity<Api.OrderResponse> response = post(body(slotId, sku, 2), UUID.randomUUID().toString(), Api.OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody().totalCents()).isEqualTo(200);
    }

    @Test
    void insufficientStockIsReportedAsProblemDetail409() {
        String sku = sku(1);
        long slotId = slot(5);

        ResponseEntity<String> response = post(body(slotId, sku, 2), UUID.randomUUID().toString(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).contains("Insufficient stock");
    }

    @Test
    void invalidBodyIsReportedAsProblemDetail400() {
        Api.PlaceOrderRequest invalid = new Api.PlaceOrderRequest("", 1L, List.of());

        ResponseEntity<String> response = post(invalid, UUID.randomUUID().toString(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    // ---------- helpers ----------

    private <T> ResponseEntity<T> post(Api.PlaceOrderRequest body, String idempotencyKey, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);
        return http.postForEntity("/api/v1/orders", new HttpEntity<>(body, headers), type);
    }

    private static Api.PlaceOrderRequest body(long slotId, String sku, int qty) {
        return new Api.PlaceOrderRequest("api-customer", slotId, List.of(new Api.Item(sku, qty)));
    }

    private String sku(int stock) {
        String sku = "API-" + UUID.randomUUID();
        products.save(new Product(sku, "API product", 100, stock));
        return sku;
    }

    private long slot(int capacity) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        return slots.save(new DeliverySlot(start, start.plus(2, ChronoUnit.HOURS), capacity)).getId();
    }
}
