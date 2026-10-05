package com.example.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String aggregateId;
    private String eventType;

    @Column(columnDefinition = "text")
    private String payload;

    private Instant createdAt;
    private Instant publishedAt;

    protected OutboxEvent() {}

    public OutboxEvent(String aggregateId, String eventType, String payload, Instant createdAt) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant at) { this.publishedAt = at; }

    public String getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
}
