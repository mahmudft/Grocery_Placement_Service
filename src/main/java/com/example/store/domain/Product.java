package com.example.store.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class Product {
    @Id
    private String sku;
    private String name;
    private long priceCents;
    private int stock;

    protected Product() {}

    public Product(String sku, String name, long priceCents, int stock) {
        this.sku = sku;
        this.name = name;
        this.priceCents = priceCents;
        this.stock = stock;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public long getPriceCents() { return priceCents; }
    public int getStock() { return stock; }
}
