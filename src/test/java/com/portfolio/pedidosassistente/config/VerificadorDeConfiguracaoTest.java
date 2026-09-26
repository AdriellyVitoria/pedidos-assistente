package com.portfolio.pedidosassistente.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificadorDeConfiguracaoTest {

    private final VerificadorDeConfiguracao verificador = new VerificadorDeConfiguracao();

    @Test
    void semConfiguracaoListaTodasAsVariaveisFaltantes() {
        MockEnvironment ambiente = new MockEnvironment();

        assertThatThrownBy(() -> verificador.postProcessEnvironment(ambiente, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD, DB_URL, DB_USERNAME, IA_API_KEY, JWT_SECRET");
    }

    @Test
    void placeholderSemValorEValorEmBrancoContamComoAusentes() {
        MockEnvironment ambiente = ambienteCompleto()
                .withProperty("app.jwt.secret", "${JWT_SECRET}")
                .withProperty("app.ia.api-key", "   ");

        assertThatThrownBy(() -> verificador.postProcessEnvironment(ambiente, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IA_API_KEY, JWT_SECRET")
                .message().doesNotContain("DB_URL");
    }

    @Test
    void comTudoConfiguradoNaoImpedeASubida() {
        assertThatCode(() -> verificador.postProcessEnvironment(ambienteCompleto(), new SpringApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    void perfilDevDispensaAVerificacao() {
        MockEnvironment ambiente = new MockEnvironment();
        ambiente.setActiveProfiles("dev");

        assertThatCode(() -> verificador.postProcessEnvironment(ambiente, new SpringApplication()))
                .doesNotThrowAnyException();
    }

    private MockEnvironment ambienteCompleto() {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://banco/pedidos")
                .withProperty("spring.datasource.username", "usuario")
                .withProperty("spring.datasource.password", "senha")
                .withProperty("app.jwt.secret", "segredo-de-producao-com-mais-de-32-bytes")
                .withProperty("app.ia.api-key", "chave");
    }
}
