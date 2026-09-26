package com.portfolio.pedidosassistente.ia;

import com.portfolio.pedidosassistente.dto.ItemPedidoResponse;
import com.portfolio.pedidosassistente.dto.PedidoResponse;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record PedidoParaIa(
        Long numero,
        String status,
        String dataDoPedido,
        BigDecimal valorTotal,
        List<Item> itens
) {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static PedidoParaIa de(PedidoResponse pedido) {
        return new PedidoParaIa(
                pedido.id(),
                pedido.status().getDescricao(),
                pedido.dataCriacao().format(FORMATO_DATA),
                pedido.valorTotal(),
                pedido.itens().stream().map(Item::de).toList()
        );
    }

    public record Item(
            String produto,
            Integer quantidade,
            BigDecimal precoUnitario,
            BigDecimal subtotal
    ) {

        static Item de(ItemPedidoResponse item) {
            return new Item(item.nomeProduto(), item.quantidade(), item.precoUnitario(), item.subtotal());
        }
    }
}
