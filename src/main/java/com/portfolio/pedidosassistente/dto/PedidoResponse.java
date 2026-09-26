package com.portfolio.pedidosassistente.dto;

import com.portfolio.pedidosassistente.model.Pedido;
import com.portfolio.pedidosassistente.model.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        StatusPedido status,
        LocalDateTime dataCriacao,
        BigDecimal valorTotal,
        List<ItemPedidoResponse> itens
) {

    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getStatus(),
                pedido.getDataCriacao(),
                pedido.getValorTotal(),
                pedido.getItens().stream().map(ItemPedidoResponse::de).toList()
        );
    }
}
