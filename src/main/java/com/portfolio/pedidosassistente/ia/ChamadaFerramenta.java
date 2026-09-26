package com.portfolio.pedidosassistente.ia;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChamadaFerramenta(
        String id,
        String type,
        FuncaoChamada function,
        @JsonProperty("extra_content") Map<String, Object> extraContent
) {
}
