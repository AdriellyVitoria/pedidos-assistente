package com.portfolio.pedidosassistente.service;

import com.portfolio.pedidosassistente.dto.CriarPedidoRequest;
import com.portfolio.pedidosassistente.dto.ItemPedidoRequest;
import com.portfolio.pedidosassistente.dto.PedidoResponse;
import com.portfolio.pedidosassistente.exception.RecursoNaoEncontradoException;
import com.portfolio.pedidosassistente.exception.RegraNegocioException;
import com.portfolio.pedidosassistente.model.ItemPedido;
import com.portfolio.pedidosassistente.model.Pedido;
import com.portfolio.pedidosassistente.model.StatusPedido;
import com.portfolio.pedidosassistente.model.Usuario;
import com.portfolio.pedidosassistente.repository.PedidoRepository;
import com.portfolio.pedidosassistente.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final Long MARIA = 1L;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PedidoService pedidoService;

    @Test
    void buscarPedidoDeOutroUsuarioLancaNaoEncontrado() {
        when(pedidoRepository.findByIdAndUsuarioId(3L, MARIA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.buscarDoUsuario(3L, MARIA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(pedidoRepository, never()).findById(anyLong());
    }

    @Test
    void criarCalculaValorTotalEAssociaAoUsuarioAutenticado() {
        Usuario maria = new Usuario();
        when(usuarioRepository.getReferenceById(MARIA)).thenReturn(maria);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        CriarPedidoRequest request = new CriarPedidoRequest(List.of(
                new ItemPedidoRequest("Mouse", 2, new BigDecimal("50.25")),
                new ItemPedidoRequest("Mousepad", 1, new BigDecimal("10.00"))));

        PedidoResponse resposta = pedidoService.criar(request, MARIA);

        ArgumentCaptor<Pedido> pedidoSalvo = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(pedidoSalvo.capture());
        assertThat(pedidoSalvo.getValue().getUsuario()).isSameAs(maria);
        assertThat(resposta.valorTotal()).isEqualByComparingTo("110.50");
        assertThat(resposta.status()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void cancelaPedidoPago() {
        Pedido pedido = pedidoComStatus(StatusPedido.PAGO);
        when(pedidoRepository.findByIdAndUsuarioId(10L, MARIA)).thenReturn(Optional.of(pedido));

        PedidoResponse resposta = pedidoService.cancelar(10L, MARIA);

        assertThat(resposta.status()).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    void naoCancelaPedidoJaEnviado() {
        Pedido pedido = pedidoComStatus(StatusPedido.ENVIADO);
        when(pedidoRepository.findByIdAndUsuarioId(10L, MARIA)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar(10L, MARIA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ENVIADO");
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.ENVIADO);
    }

    @Test
    void naoCancelaPedidoDeOutroUsuario() {
        when(pedidoRepository.findByIdAndUsuarioId(3L, MARIA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.cancelar(3L, MARIA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Pedido pedidoComStatus(StatusPedido status) {
        Pedido pedido = new Pedido();
        pedido.setId(10L);
        pedido.setStatus(status);
        pedido.setDataCriacao(LocalDateTime.of(2026, 9, 25, 10, 0));
        pedido.adicionarItem(new ItemPedido("Livro", 1, new BigDecimal("89.00")));
        return pedido;
    }
}
