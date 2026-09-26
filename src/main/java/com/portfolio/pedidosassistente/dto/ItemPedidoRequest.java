package com.portfolio.pedidosassistente.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemPedidoRequest(
        @NotBlank @Size(max = 150) String nomeProduto,
        @NotNull @Min(1) Integer quantidade,
        @NotNull @DecimalMin("0.01") BigDecimal precoUnitario
) {
}
