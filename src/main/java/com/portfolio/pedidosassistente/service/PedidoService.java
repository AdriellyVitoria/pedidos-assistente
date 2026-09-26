package com.portfolio.pedidosassistente.service;

import com.portfolio.pedidosassistente.dto.CriarPedidoRequest;
import com.portfolio.pedidosassistente.dto.PedidoResponse;
import com.portfolio.pedidosassistente.exception.RecursoNaoEncontradoException;
import com.portfolio.pedidosassistente.exception.RegraNegocioException;
import com.portfolio.pedidosassistente.model.ItemPedido;
import com.portfolio.pedidosassistente.model.Pedido;
import com.portfolio.pedidosassistente.model.StatusPedido;
import com.portfolio.pedidosassistente.repository.PedidoRepository;
import com.portfolio.pedidosassistente.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final Set<StatusPedido> STATUS_CANCELAVEIS =
            Set.of(StatusPedido.AGUARDANDO_PAGAMENTO, StatusPedido.PAGO);

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarDoUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId).stream()
                .map(PedidoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarDoUsuario(Long pedidoId, Long usuarioId) {
        return PedidoResponse.de(buscarPedidoDoUsuario(pedidoId, usuarioId));
    }

    @Transactional
    public PedidoResponse criar(CriarPedidoRequest request, Long usuarioId) {
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        request.itens().forEach(item ->
                pedido.adicionarItem(new ItemPedido(item.nomeProduto(), item.quantidade(), item.precoUnitario())));

        return PedidoResponse.de(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse cancelar(Long pedidoId, Long usuarioId) {
        Pedido pedido = buscarPedidoDoUsuario(pedidoId, usuarioId);
        if (!STATUS_CANCELAVEIS.contains(pedido.getStatus())) {
            throw new RegraNegocioException("Pedido com status " + pedido.getStatus() + " não pode ser cancelado");
        }
        pedido.setStatus(StatusPedido.CANCELADO);
        return PedidoResponse.de(pedido);
    }

    @Transactional
    public PedidoResponse atualizarStatus(Long pedidoId, StatusPedido novoStatus) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido " + pedidoId + " não encontrado"));
        pedido.setStatus(novoStatus);
        return PedidoResponse.de(pedido);
    }

    private Pedido buscarPedidoDoUsuario(Long pedidoId, Long usuarioId) {
        return pedidoRepository.findByIdAndUsuarioId(pedidoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido " + pedidoId + " não encontrado"));
    }
}
