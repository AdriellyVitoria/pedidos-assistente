package com.portfolio.pedidosassistente.ia;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModelosIaJsonTest {

    private static final String RESPOSTA_REAL_DO_GEMINI = """
            {
              "choices": [{
                "finish_reason": "tool_calls",
                "index": 0,
                "message": {
                  "role": "assistant",
                  "tool_calls": [{
                    "extra_content": {"google": {"thought_signature": "EtUDCtIDAWkUfRM1jwyF"}},
                    "function": {"arguments": "{\\"pedidoId\\":1}", "name": "buscarPedido"},
                    "id": "call_415423",
                    "type": "function"
                  }]
                }
              }],
              "created": 1790389002,
              "id": "_Sq3auatLaTBqtsPtpLImQQ",
              "model": "gemini-flash-latest",
              "object": "chat.completion",
              "usage": {"completion_tokens": 14, "prompt_tokens": 97, "total_tokens": 209}
            }
            """;

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void leARespostaDoGeminiIgnorandoCamposDesconhecidos() {
        MensagemIa mensagem = lerMensagemDaResposta();

        assertThat(mensagem.pediuFerramentas()).isTrue();
        ChamadaFerramenta chamada = mensagem.toolCalls().getFirst();
        assertThat(chamada.id()).isEqualTo("call_415423");
        assertThat(chamada.function().name()).isEqualTo("buscarPedido");
        assertThat(chamada.function().arguments()).isEqualTo("{\"pedidoId\":1}");
    }

    @Test
    void devolveAThoughtSignatureExatamenteComoRecebeu() {
        MensagemIa mensagemDaIa = lerMensagemDaResposta();
        RequisicaoIa proximaRodada = new RequisicaoIa("modelo", List.of(
                MensagemIa.usuario("status do pedido 1?"),
                mensagemDaIa,
                MensagemIa.resultadoFerramenta("call_415423", "{\"status\":\"Pago\"}")), List.of());

        String json = jsonMapper.writeValueAsString(proximaRodada);

        assertThat((String) JsonPath.read(json, "$.messages[1].tool_calls[0].extra_content.google.thought_signature"))
                .isEqualTo("EtUDCtIDAWkUfRM1jwyF");
        assertThat((String) JsonPath.read(json, "$.messages[1].tool_calls[0].function.arguments"))
                .isEqualTo("{\"pedidoId\":1}");
        assertThat((String) JsonPath.read(json, "$.messages[2].tool_call_id")).isEqualTo("call_415423");
        assertThat((String) JsonPath.read(json, "$.messages[2].role")).isEqualTo("tool");
    }

    @Test
    void naoEnviaCamposNulosParaAIa() {
        String json = jsonMapper.writeValueAsString(MensagemIa.usuario("oi"));

        Map<String, Object> mensagem = JsonPath.read(json, "$");
        assertThat(mensagem).containsOnlyKeys("role", "content");
    }

    private MensagemIa lerMensagemDaResposta() {
        return jsonMapper.readValue(RESPOSTA_REAL_DO_GEMINI, RespostaIa.class).choices().getFirst().message();
    }
}
