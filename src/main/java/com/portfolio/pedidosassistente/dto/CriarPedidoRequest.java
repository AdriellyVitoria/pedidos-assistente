package com.portfolio.pedidosassistente.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CriarPedidoRequest(
        @NotEmpty @Valid List<ItemPedidoRequest> itens
) {
}
