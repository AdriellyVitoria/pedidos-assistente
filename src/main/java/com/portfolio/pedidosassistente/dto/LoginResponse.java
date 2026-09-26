package com.portfolio.pedidosassistente.dto;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEmSegundos
) {
}
