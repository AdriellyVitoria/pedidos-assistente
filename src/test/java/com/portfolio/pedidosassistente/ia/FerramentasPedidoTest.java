package com.portfolio.pedidosassistente.ia;

import com.portfolio.pedidosassistente.dto.ItemPedidoResponse;
import com.portfolio.pedidosassistente.dto.PedidoResponse;
import com.portfolio.pedidosassistente.exception.RecursoNaoEncontradoException;
import com.portfolio.pedidosassistente.model.StatusPedido;
import com.portfolio.pedidosassistente.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FerramentasPedidoTest {

    private static final Long MARIA = 1L;

    @Mock
    private PedidoService pedidoService;

    private FerramentasPedido ferramentas;

    @BeforeEach
    void configurar() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        ferramentas = new FerramentasPedido(pedidoService, jsonMapper);
    }

    @Test
    void listarUsaOUsuarioAutenticadoEDevolveStatusHumanizado() {
        when(pedidoService.listarDoUsuario(MARIA)).thenReturn(List.of(pedido(4L, StatusPedido.AGUARDANDO_PAGAMENTO)));

        String resultado = ferramentas.executar(new FuncaoChamada("listarMeusPedidos", "{}"), MARIA);

        verify(pedidoService).listarDoUsuario(MARIA);
        assertThat(resultado)
                .contains("\"numero\":4")
                .contains("Aguardando pagamento")
                .contains("25/09/2026")
                .doesNotContain("AGUARDANDO_PAGAMENTO");
    }

    @Test
    void ignoraUsuarioIdEnviadoPelaIa() {
        when(pedidoService.buscarDoUsuario(4L, MARIA)).thenReturn(pedido(4L, StatusPedido.PAGO));

        ferramentas.executar(new FuncaoChamada("buscarPedido", "{\"pedidoId\":4,\"usuarioId\":2}"), MARIA);

        verify(pedidoService).buscarDoUsuario(4L, MARIA);
    }

    @Test
    void pedidoNaoEncontradoViraErroParaAIa() {
        when(pedidoService.buscarDoUsuario(3L, MARIA))
                .thenThrow(new RecursoNaoEncontradoException("Pedido 3 não encontrado"));

        String resultado = ferramentas.executar(new FuncaoChamada("buscarPedido", "{\"pedidoId\":3}"), MARIA);

        assertThat(resultado).contains("\"erro\"").contains("Pedido 3 não encontrado");
    }

    @Test
    void aceitaPedidoIdEnviadoComoTexto() {
        when(pedidoService.buscarDoUsuario(4L, MARIA)).thenReturn(pedido(4L, StatusPedido.PAGO));

        String resultado = ferramentas.executar(new FuncaoChamada("buscarPedido", "{\"pedidoId\":\"4\"}"), MARIA);

        assertThat(resultado).contains("\"status\":\"Pago\"");
    }

    @Test
    void argumentosInvalidosViramErroSemConsultarOBanco() {
        String semId = ferramentas.executar(new FuncaoChamada("buscarPedido", "{}"), MARIA);
        String jsonQuebrado = ferramentas.executar(new FuncaoChamada("buscarPedido", "isso não é json"), MARIA);

        assertThat(semId).contains("Argumentos inválidos");
        assertThat(jsonQuebrado).contains("Argumentos inválidos");
        verifyNoInteractions(pedidoService);
    }

    @Test
    void funcaoDesconhecidaViraErroSemConsultarOBanco() {
        String resultado = ferramentas.executar(new FuncaoChamada("apagarTodosOsPedidos", "{}"), MARIA);

        assertThat(resultado).contains("Função desconhecida");
        verifyNoInteractions(pedidoService);
    }

    @Test
    void nenhumaFuncaoExpostaRecebeUsuarioId() {
        assertThat(ferramentas.definicoes())
                .extracting(definicao -> definicao.function().parameters().toString())
                .noneMatch(parametros -> parametros.toLowerCase().contains("usuario"));
    }

    private PedidoResponse pedido(Long id, StatusPedido status) {
        return new PedidoResponse(
                id,
                status,
                LocalDateTime.of(2026, 9, 25, 10, 0),
                new BigDecimal("100.50"),
                List.of(new ItemPedidoResponse("Mouse", 2, new BigDecimal("50.25"), new BigDecimal("100.50"))));
    }
}
