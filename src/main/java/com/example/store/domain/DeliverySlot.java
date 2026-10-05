package com.example.store.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "delivery_slot")
public class DeliverySlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant startsAt;
    private Instant endsAt;
    private int capacity;
    private int booked;

    protected DeliverySlot() {}

    public DeliverySlot(Instant startsAt, Instant endsAt, int capacity) {
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.capacity = capacity;
    }

    public Long getId() { return id; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public int getCapacity() { return capacity; }
    public int getBooked() { return booked; }
}
