package com.portfolio.pedidosassistente.dto;

import com.portfolio.pedidosassistente.model.ItemPedido;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        String nomeProduto,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {

    public static ItemPedidoResponse de(ItemPedido item) {
        return new ItemPedidoResponse(
                item.getNomeProduto(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal()
        );
    }
}
