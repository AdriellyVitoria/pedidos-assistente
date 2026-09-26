package com.portfolio.pedidosassistente.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.chat")
public record ChatProperties(
        int limitePorMinuto,
        int limitePorDia,
        int mensagensDeContexto,
        Duration janelaDeContexto
) {
}
