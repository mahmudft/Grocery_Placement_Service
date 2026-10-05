package com.example.store.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID orderId,
        String customerId,
        Long slotId,
        long totalCents,
        Instant occurredAt,
        List<Line> lines) {

    public record Line(String sku, int quantity) {}
}
