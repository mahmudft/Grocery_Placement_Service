package com.example.store.config;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "store")
public record StoreProperties(ZoneId zone, Duration slotHoldTtl, Topics topics) {
    public record Topics(String orderPlaced) {}
}
