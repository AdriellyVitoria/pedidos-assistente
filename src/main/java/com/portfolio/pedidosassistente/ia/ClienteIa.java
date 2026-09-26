package com.portfolio.pedidosassistente.ia;

import com.portfolio.pedidosassistente.exception.IaIndisponivelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class ClienteIa {

    static final String MENSAGEM_COTA =
            "O assistente está recebendo muitas perguntas agora. Tente novamente em alguns instantes.";
    static final String MENSAGEM_INDISPONIVEL =
            "O assistente está indisponível no momento. Tente novamente mais tarde.";

    private final RestClient restClient;
    private final IaProperties propriedades;

    public ClienteIa(RestClient.Builder builder, IaProperties propriedades) {
        this.propriedades = propriedades;

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(propriedades.timeout());

        this.restClient = builder
                .baseUrl(propriedades.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + propriedades.apiKey())
                .requestFactory(fabrica)
                .build();
    }

    public MensagemIa enviar(List<MensagemIa> mensagens, List<DefinicaoFerramenta> ferramentas) {
        if (!StringUtils.hasText(propriedades.apiKey())) {
            log.error("Chave da IA não configurada (variável IA_API_KEY)");
            throw new IaIndisponivelException(HttpStatus.SERVICE_UNAVAILABLE, MENSAGEM_INDISPONIVEL);
        }

        try {
            RespostaIa resposta = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RequisicaoIa(propriedades.modelo(), mensagens, ferramentas))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (requisicao, respostaErro) -> tratarErro(respostaErro))
                    .body(RespostaIa.class);

            if (resposta == null || resposta.choices() == null || resposta.choices().isEmpty()) {
                log.warn("IA retornou uma resposta sem conteúdo");
                throw new IaIndisponivelException(HttpStatus.SERVICE_UNAVAILABLE, MENSAGEM_INDISPONIVEL);
            }
            return resposta.choices().getFirst().message();
        } catch (ResourceAccessException ex) {
            log.warn("Falha de comunicação com a IA: {}", ex.getMessage());
            throw new IaIndisponivelException(HttpStatus.SERVICE_UNAVAILABLE, MENSAGEM_INDISPONIVEL);
        }
    }

    private void tratarErro(ClientHttpResponse resposta) throws IOException {
        int status = resposta.getStatusCode().value();
        String corpo = new String(resposta.getBody().readAllBytes(), StandardCharsets.UTF_8);
        log.warn("IA respondeu com HTTP {}: {}", status, corpo);

        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            throw new IaIndisponivelException(HttpStatus.TOO_MANY_REQUESTS, MENSAGEM_COTA);
        }
        throw new IaIndisponivelException(HttpStatus.SERVICE_UNAVAILABLE, MENSAGEM_INDISPONIVEL);
    }
}
