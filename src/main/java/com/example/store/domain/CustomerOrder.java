package com.example.store.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customer_order")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String customerId;
    private Long slotId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private long totalCents;
    private String idempotencyKey;
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    protected CustomerOrder() {}

    public CustomerOrder(String customerId, Long slotId, String idempotencyKey, Instant createdAt) {
        this.customerId = customerId;
        this.slotId = slotId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.status = OrderStatus.PLACED;
    }

    public void addLine(String sku, int quantity, long unitPriceCents) {
        lines.add(new OrderLine(this, sku, quantity, unitPriceCents));
        totalCents += quantity * unitPriceCents;
    }

    /** Idempotent state transition: confirming twice is a no-op, so event redelivery is harmless. */
    public boolean confirm() {
        if (status == OrderStatus.CONFIRMED) {
            return false;
        }
        status = OrderStatus.CONFIRMED;
        return true;
    }

    public UUID getId() { return id; }
    public String getCustomerId() { return customerId; }
    public Long getSlotId() { return slotId; }
    public OrderStatus getStatus() { return status; }
    public long getTotalCents() { return totalCents; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderLine> getLines() { return lines; }
}
