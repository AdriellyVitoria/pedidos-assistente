package com.portfolio.pedidosassistente.ia;

import java.util.Map;

public record DefinicaoFerramenta(
        String type,
        Funcao function
) {

    public static DefinicaoFerramenta funcao(String nome, String descricao, Map<String, Object> parametros) {
        return new DefinicaoFerramenta("function", new Funcao(nome, descricao, parametros));
    }

    public record Funcao(
            String name,
            String description,
            Map<String, Object> parameters
    ) {
    }
}
