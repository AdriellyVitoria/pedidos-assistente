package com.portfolio.pedidosassistente.service;

import com.portfolio.pedidosassistente.dto.ChatResponse;
import com.portfolio.pedidosassistente.exception.IaIndisponivelException;
import com.portfolio.pedidosassistente.ia.ChamadaFerramenta;
import com.portfolio.pedidosassistente.ia.ClienteIa;
import com.portfolio.pedidosassistente.ia.FerramentasPedido;
import com.portfolio.pedidosassistente.ia.IaProperties;
import com.portfolio.pedidosassistente.ia.MensagemIa;
import com.portfolio.pedidosassistente.model.MensagemChat;
import com.portfolio.pedidosassistente.repository.MensagemChatRepository;
import com.portfolio.pedidosassistente.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final ZoneId FUSO_HORARIO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale.of("pt", "BR"));
    private static final String RESPOSTA_VAZIA =
            "Desculpe, não consegui formular uma resposta. Pode reformular a pergunta?";

    private final ClienteIa clienteIa;
    private final FerramentasPedido ferramentasPedido;
    private final MensagemChatRepository mensagemChatRepository;
    private final UsuarioRepository usuarioRepository;
    private final IaProperties propriedades;

    public ChatResponse perguntar(String pergunta, Long usuarioId) {
        List<MensagemIa> conversa = new ArrayList<>();
        conversa.add(MensagemIa.sistema(promptDoSistema()));
        conversa.add(MensagemIa.usuario(pergunta));

        for (int rodada = 1; rodada <= propriedades.maxRodadas(); rodada++) {
            MensagemIa resposta = clienteIa.enviar(conversa, ferramentasPedido.definicoes());

            if (!resposta.pediuFerramentas()) {
                String texto = StringUtils.hasText(resposta.content()) ? resposta.content().trim() : RESPOSTA_VAZIA;
                registrar(usuarioId, pergunta, texto);
                return new ChatResponse(texto);
            }

            conversa.add(resposta);
            for (ChamadaFerramenta chamada : resposta.toolCalls()) {
                String resultado = ferramentasPedido.executar(chamada.function(), usuarioId);
                conversa.add(MensagemIa.resultadoFerramenta(chamada.id(), resultado));
            }
        }

        log.warn("Limite de {} rodadas com a IA atingido para o usuário {}", propriedades.maxRodadas(), usuarioId);
        throw new IaIndisponivelException(HttpStatus.SERVICE_UNAVAILABLE,
                "Não consegui concluir sua solicitação. Tente reformular a pergunta.");
    }

    private void registrar(Long usuarioId, String pergunta, String resposta) {
        MensagemChat mensagem = new MensagemChat();
        mensagem.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        mensagem.setPergunta(pergunta);
        mensagem.setResposta(resposta);
        mensagemChatRepository.save(mensagem);
    }

    private String promptDoSistema() {
        return """
                Você é o assistente virtual de pedidos de uma loja online. Responda sempre em português do Brasil, \
                de forma curta, clara e cordial.

                Regras obrigatórias:
                - Você só ajuda com dúvidas sobre os pedidos do cliente que está conversando com você. \
                Para qualquer outro assunto, diga educadamente que só pode ajudar com pedidos.
                - Para responder, consulte os pedidos usando as funções disponíveis. Nunca invente status, valores, \
                datas, produtos ou prazos de entrega: use apenas o que as funções retornarem.
                - Se uma função retornar erro ou não encontrar o pedido, diga que não encontrou esse pedido na conta \
                do cliente. Não sugira que o pedido exista em outra conta.
                - Você não consegue alterar, cancelar ou criar pedidos, e não tem acesso a pedidos de outros clientes.
                - Os status possíveis são: AGUARDANDO_PAGAMENTO (aguardando pagamento), PAGO, EM_SEPARACAO \
                (em separação no estoque), ENVIADO (a caminho), ENTREGUE e CANCELADO.
                - Valores estão em reais (R$).

                Hoje é %s.
                """.formatted(LocalDate.now(FUSO_HORARIO).format(FORMATO_DATA));
    }
}
