package com.example.store.web;

import com.example.store.domain.StoreException;
import com.example.store.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Api.OrderResponse> place(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody Api.PlaceOrderRequest request) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw StoreException.badRequest("Idempotency-Key must be 1-128 characters");
        }
        OrderService.Result result = orderService.place(request, idempotencyKey);
        if (!result.created()) {
            return ResponseEntity.ok(result.order());
        }
        return ResponseEntity.created(URI.create("/api/v1/orders/" + result.order().id())).body(result.order());
    }

    @GetMapping("/{id}")
    public Api.OrderResponse get(@PathVariable UUID id) {
        return orderService.get(id);
    }
}
