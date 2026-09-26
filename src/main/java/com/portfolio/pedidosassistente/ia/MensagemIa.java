package com.portfolio.pedidosassistente.ia;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MensagemIa(
        String role,
        String content,
        @JsonProperty("tool_calls") List<ChamadaFerramenta> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId
) {

    public static MensagemIa sistema(String conteudo) {
        return new MensagemIa("system", conteudo, null, null);
    }

    public static MensagemIa usuario(String conteudo) {
        return new MensagemIa("user", conteudo, null, null);
    }

    public static MensagemIa assistente(String conteudo) {
        return new MensagemIa("assistant", conteudo, null, null);
    }

    public static MensagemIa resultadoFerramenta(String toolCallId, String conteudo) {
        return new MensagemIa("tool", conteudo, null, toolCallId);
    }

    public boolean pediuFerramentas() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
