package com.example.store.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_line")
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private CustomerOrder order;

    private String sku;
    private int quantity;
    private long unitPriceCents;

    protected OrderLine() {}

    OrderLine(CustomerOrder order, String sku, int quantity, long unitPriceCents) {
        this.order = order;
        this.sku = sku;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public long getUnitPriceCents() { return unitPriceCents; }
}
