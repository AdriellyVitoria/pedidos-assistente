package com.portfolio.pedidosassistente.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ia")
public record IaProperties(
        String baseUrl,
        String apiKey,
        String modelo,
        int maxRodadas,
        Duration timeout
) {
}
