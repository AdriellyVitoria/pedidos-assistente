package com.portfolio.pedidosassistente.dto;

import com.portfolio.pedidosassistente.model.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusRequest(
        @NotNull StatusPedido status
) {
}
