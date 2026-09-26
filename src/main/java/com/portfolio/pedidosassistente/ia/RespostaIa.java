package com.portfolio.pedidosassistente.ia;

import java.util.List;

public record RespostaIa(
        List<Escolha> choices
) {

    public record Escolha(
            MensagemIa message
    ) {
    }
}
