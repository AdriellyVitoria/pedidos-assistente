package com.portfolio.pedidosassistente.ia;

import java.util.List;

public record RequisicaoIa(
        String model,
        List<MensagemIa> messages,
        List<DefinicaoFerramenta> tools
) {
}
