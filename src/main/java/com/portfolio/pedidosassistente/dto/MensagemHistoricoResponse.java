package com.portfolio.pedidosassistente.dto;

import com.portfolio.pedidosassistente.model.MensagemChat;

import java.time.LocalDateTime;

public record MensagemHistoricoResponse(
        String pergunta,
        String resposta,
        LocalDateTime timestamp
) {

    public static MensagemHistoricoResponse de(MensagemChat mensagem) {
        return new MensagemHistoricoResponse(mensagem.getPergunta(), mensagem.getResposta(), mensagem.getTimestamp());
    }
}
