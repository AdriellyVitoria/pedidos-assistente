package com.portfolio.pedidosassistente.service;

import com.portfolio.pedidosassistente.config.ChatProperties;
import com.portfolio.pedidosassistente.dto.ChatResponse;
import com.portfolio.pedidosassistente.exception.IaIndisponivelException;
import com.portfolio.pedidosassistente.exception.LimiteDePerguntasException;
import com.portfolio.pedidosassistente.ia.ChamadaFerramenta;
import com.portfolio.pedidosassistente.ia.ClienteIa;
import com.portfolio.pedidosassistente.ia.FerramentasPedido;
import com.portfolio.pedidosassistente.ia.FuncaoChamada;
import com.portfolio.pedidosassistente.ia.IaProperties;
import com.portfolio.pedidosassistente.ia.MensagemIa;
import com.portfolio.pedidosassistente.model.MensagemChat;
import com.portfolio.pedidosassistente.repository.MensagemChatRepository;
import com.portfolio.pedidosassistente.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final Long MARIA = 1L;
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 26, 12, 0);

    @Mock
    private ClienteIa clienteIa;

    @Mock
    private FerramentasPedido ferramentasPedido;

    @Mock
    private LimitadorDePerguntas limitadorDePerguntas;

    @Mock
    private MensagemChatRepository mensagemChatRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Captor
    private ArgumentCaptor<List<MensagemIa>> conversa;

    private ChatService chatService;

    @BeforeEach
    void configurar() {
        Clock relogio = Clock.fixed(Instant.parse("2026-09-26T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        chatService = new ChatService(
                clienteIa,
                ferramentasPedido,
                limitadorDePerguntas,
                mensagemChatRepository,
                usuarioRepository,
                new IaProperties("http://ia.local", "chave", "modelo", 3, Duration.ofSeconds(5)),
                new ChatProperties(10, 50, 5, Duration.ofMinutes(30)),
                relogio);
        lenient().when(mensagemChatRepository.findByUsuarioIdAndTimestampAfterOrderByTimestampDesc(any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void respostaDiretaEhDevolvidaERegistradaNaAuditoria() {
        when(clienteIa.enviar(anyList(), anyList())).thenReturn(MensagemIa.assistente("Seu pedido foi enviado."));

        ChatResponse resposta = chatService.perguntar("cadê meu pedido?", MARIA);

        assertThat(resposta.resposta()).isEqualTo("Seu pedido foi enviado.");
        ArgumentCaptor<MensagemChat> registro = ArgumentCaptor.forClass(MensagemChat.class);
        verify(mensagemChatRepository).save(registro.capture());
        assertThat(registro.getValue().getPergunta()).isEqualTo("cadê meu pedido?");
        assertThat(registro.getValue().getResposta()).isEqualTo("Seu pedido foi enviado.");
        assertThat(registro.getValue().getTimestamp()).isEqualTo(AGORA);
    }

    @Test
    void executaFuncaoComUsuarioAutenticadoEDevolveOResultadoParaAIa() {
        FuncaoChamada funcao = new FuncaoChamada("buscarPedido", "{\"pedidoId\":4}");
        Map<String, Object> assinatura = Map.of("google", Map.of("thought_signature", "assinatura-123"));
        MensagemIa pedidoDeFuncao = new MensagemIa("assistant", null,
                List.of(new ChamadaFerramenta("call_1", "function", funcao, assinatura)), null);
        when(clienteIa.enviar(anyList(), anyList()))
                .thenReturn(pedidoDeFuncao)
                .thenReturn(MensagemIa.assistente("O pedido 4 está Pago."));
        when(ferramentasPedido.executar(funcao, MARIA)).thenReturn("{\"status\":\"Pago\"}");

        ChatResponse resposta = chatService.perguntar("status do pedido 4?", MARIA);

        assertThat(resposta.resposta()).isEqualTo("O pedido 4 está Pago.");
        verify(ferramentasPedido).executar(funcao, MARIA);
        verify(clienteIa, times(2)).enviar(conversa.capture(), anyList());
        List<MensagemIa> mensagens = conversa.getValue();
        assertThat(mensagens).anySatisfy(mensagem -> {
            assertThat(mensagem.role()).isEqualTo("tool");
            assertThat(mensagem.toolCallId()).isEqualTo("call_1");
            assertThat(mensagem.content()).isEqualTo("{\"status\":\"Pago\"}");
        });
        assertThat(mensagens).anySatisfy(mensagem ->
                assertThat(mensagem.toolCalls()).first().extracting(ChamadaFerramenta::extraContent).isEqualTo(assinatura));
    }

    @Test
    void interrompeQuandoAIaPassaDoLimiteDeRodadas() {
        FuncaoChamada funcao = new FuncaoChamada("listarMeusPedidos", "{}");
        MensagemIa pedidoDeFuncao = new MensagemIa("assistant", null,
                List.of(new ChamadaFerramenta("call_1", "function", funcao, null)), null);
        when(clienteIa.enviar(anyList(), anyList())).thenReturn(pedidoDeFuncao);
        when(ferramentasPedido.executar(funcao, MARIA)).thenReturn("[]");

        assertThatThrownBy(() -> chatService.perguntar("meus pedidos", MARIA))
                .isInstanceOf(IaIndisponivelException.class);
        verify(clienteIa, times(3)).enviar(anyList(), anyList());
        verify(mensagemChatRepository, never()).save(any());
    }

    @Test
    void limiteDePerguntasImpedeAChamadaAIa() {
        doThrow(new LimiteDePerguntasException("limite")).when(limitadorDePerguntas).registrarPergunta(MARIA);

        assertThatThrownBy(() -> chatService.perguntar("oi", MARIA))
                .isInstanceOf(LimiteDePerguntasException.class);
        verifyNoInteractions(clienteIa);
    }

    @Test
    void historicoRecenteEntraNaConversaEmOrdemCronologica() {
        MensagemChat maisAntiga = mensagem("qual o status do pedido 4?", "Pago.");
        MensagemChat maisRecente = mensagem("e quanto custou?", "R$ 100,50.");
        when(mensagemChatRepository.findByUsuarioIdAndTimestampAfterOrderByTimestampDesc(
                eq(MARIA), eq(AGORA.minusMinutes(30)), eq(Limit.of(5))))
                .thenReturn(List.of(maisRecente, maisAntiga));
        when(clienteIa.enviar(anyList(), anyList())).thenReturn(MensagemIa.assistente("Tinha 2 mouses."));

        chatService.perguntar("o que tinha nele?", MARIA);

        verify(clienteIa).enviar(conversa.capture(), anyList());
        assertThat(conversa.getValue())
                .extracting(MensagemIa::role, MensagemIa::content)
                .containsExactly(
                        tuple("system", conversa.getValue().getFirst().content()),
                        tuple("user", "qual o status do pedido 4?"),
                        tuple("assistant", "Pago."),
                        tuple("user", "e quanto custou?"),
                        tuple("assistant", "R$ 100,50."),
                        tuple("user", "o que tinha nele?"));
    }

    @Test
    void promptDoSistemaInformaADataDeHoje() {
        when(clienteIa.enviar(anyList(), anyList())).thenReturn(MensagemIa.assistente("Olá!"));

        chatService.perguntar("oi", MARIA);

        verify(clienteIa).enviar(conversa.capture(), anyList());
        assertThat(conversa.getValue().getFirst().content()).contains("26/09/2026");
    }

    private MensagemChat mensagem(String pergunta, String resposta) {
        MensagemChat mensagem = new MensagemChat();
        mensagem.setPergunta(pergunta);
        mensagem.setResposta(resposta);
        return mensagem;
    }
}
