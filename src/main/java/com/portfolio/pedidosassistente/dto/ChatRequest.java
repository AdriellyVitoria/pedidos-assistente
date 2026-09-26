package com.portfolio.pedidosassistente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Size(max = 500) String pergunta
) {
}
