package com.example.store.web;

import com.example.store.domain.CustomerOrder;
import com.example.store.domain.OrderStatus;
import com.example.store.domain.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Api {
    private Api() {}

    public record Item(@NotBlank String sku, @Min(1) @Max(99) int quantity) {}

    public record PlaceOrderRequest(
            @NotBlank String customerId,
            @NotNull Long slotId,
            @NotEmpty List<@Valid Item> items) {}

    public record LineResponse(String sku, int quantity, long unitPriceCents) {}

    public record OrderResponse(
            UUID id, String customerId, Long slotId, OrderStatus status,
            long totalCents, Instant createdAt, List<LineResponse> lines) {

        public static OrderResponse from(CustomerOrder o) {
            return new OrderResponse(o.getId(), o.getCustomerId(), o.getSlotId(), o.getStatus(),
                    o.getTotalCents(), o.getCreatedAt(),
                    o.getLines().stream()
                            .map(l -> new LineResponse(l.getSku(), l.getQuantity(), l.getUnitPriceCents()))
                            .toList());
        }
    }

    public record ProductResponse(String sku, String name, long priceCents, int stock) {
        public static ProductResponse from(Product p) {
            return new ProductResponse(p.getSku(), p.getName(), p.getPriceCents(), p.getStock());
        }
    }

    public record SlotAvailability(
            long id, Instant startsAt, Instant endsAt, int capacity, int booked, int activeHolds, int available) {}

    public record HoldRequest(@NotBlank String customerId) {}

    public record HoldResponse(long slotId, String customerId, Instant expiresAt) {}
}
