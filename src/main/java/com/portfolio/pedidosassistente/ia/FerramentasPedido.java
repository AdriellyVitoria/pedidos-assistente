package com.portfolio.pedidosassistente.ia;

import com.portfolio.pedidosassistente.exception.RecursoNaoEncontradoException;
import com.portfolio.pedidosassistente.service.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FerramentasPedido {

    static final String LISTAR_MEUS_PEDIDOS = "listarMeusPedidos";
    static final String BUSCAR_PEDIDO = "buscarPedido";

    private final PedidoService pedidoService;
    private final JsonMapper jsonMapper;

    public List<DefinicaoFerramenta> definicoes() {
        return List.of(
                DefinicaoFerramenta.funcao(
                        LISTAR_MEUS_PEDIDOS,
                        "Lista todos os pedidos do cliente logado, do mais recente para o mais antigo, "
                                + "com número, status, data de criação, valor total e itens.",
                        Map.of("type", "object", "properties", Map.of())),
                DefinicaoFerramenta.funcao(
                        BUSCAR_PEDIDO,
                        "Busca um pedido específico do cliente logado pelo número do pedido.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "pedidoId", Map.of("type", "integer", "description", "Número do pedido")),
                                "required", List.of("pedidoId")))
        );
    }

    public String executar(FuncaoChamada funcao, Long usuarioId) {
        log.info("IA solicitou {}({}) para o usuário {}", funcao.name(), funcao.arguments(), usuarioId);
        try {
            Object resultado = switch (funcao.name()) {
                case LISTAR_MEUS_PEDIDOS -> pedidoService.listarDoUsuario(usuarioId).stream()
                        .map(PedidoParaIa::de)
                        .toList();
                case BUSCAR_PEDIDO -> PedidoParaIa.de(pedidoService.buscarDoUsuario(lerPedidoId(funcao), usuarioId));
                default -> Map.of("erro", "Função desconhecida: " + funcao.name());
            };
            return jsonMapper.writeValueAsString(resultado);
        } catch (RecursoNaoEncontradoException ex) {
            return erro(ex.getMessage());
        } catch (JacksonException | IllegalArgumentException ex) {
            return erro("Argumentos inválidos para a função " + funcao.name());
        }
    }

    private Long lerPedidoId(FuncaoChamada funcao) {
        ArgumentosBuscarPedido argumentos = jsonMapper.readValue(funcao.arguments(), ArgumentosBuscarPedido.class);
        if (argumentos.pedidoId() == null) {
            throw new IllegalArgumentException("pedidoId ausente");
        }
        return argumentos.pedidoId();
    }

    private String erro(String mensagem) {
        return jsonMapper.writeValueAsString(Map.of("erro", mensagem));
    }

    record ArgumentosBuscarPedido(Long pedidoId) {
    }
}
