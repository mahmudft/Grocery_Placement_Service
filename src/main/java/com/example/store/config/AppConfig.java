package com.example.store.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {

    /** Injected everywhere instead of Instant.now() so time-dependent logic stays testable. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OpenAPI openApi() {
        return new OpenAPI().info(new Info()
                .title("Grocery Order Fulfillment API")
                .version("v1")
                .description("Checkout with atomic stock reservation, delivery slot holds and event streaming"));
    }
}
