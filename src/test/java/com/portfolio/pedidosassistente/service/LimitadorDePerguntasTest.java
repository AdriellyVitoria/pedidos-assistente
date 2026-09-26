package com.portfolio.pedidosassistente.service;

import com.portfolio.pedidosassistente.config.ChatProperties;
import com.portfolio.pedidosassistente.exception.LimiteDePerguntasException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitadorDePerguntasTest {

    private static final Long MARIA = 1L;
    private static final Long JOAO = 2L;

    private final RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-09-26T15:00:00Z"));
    private final LimitadorDePerguntas limitador =
            new LimitadorDePerguntas(new ChatProperties(3, 5, 5, Duration.ofMinutes(30)), relogio);

    @Test
    void permitePerguntasAteOLimitePorMinuto() {
        assertThatCode(() -> registrarVezes(MARIA, 3)).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaQuandoPassaDoLimitePorMinuto() {
        registrarVezes(MARIA, 3);

        assertThatThrownBy(() -> limitador.registrarPergunta(MARIA))
                .isInstanceOf(LimiteDePerguntasException.class)
                .hasMessageContaining("muito rápido");
    }

    @Test
    void liberaNovamenteDepoisDeUmMinuto() {
        registrarVezes(MARIA, 3);
        relogio.avancar(Duration.ofSeconds(61));

        assertThatCode(() -> limitador.registrarPergunta(MARIA)).doesNotThrowAnyException();
    }

    @Test
    void bloqueiaQuandoPassaDoLimitePorDia() {
        registrarEspacadoVezes(MARIA, 5);

        assertThatThrownBy(() -> limitador.registrarPergunta(MARIA))
                .isInstanceOf(LimiteDePerguntasException.class)
                .hasMessageContaining("por dia");
    }

    @Test
    void zeraOLimiteDiarioNoDiaSeguinte() {
        registrarEspacadoVezes(MARIA, 5);
        relogio.avancar(Duration.ofDays(1));

        assertThatCode(() -> limitador.registrarPergunta(MARIA)).doesNotThrowAnyException();
    }

    @Test
    void limiteDeUmUsuarioNaoAfetaOutro() {
        registrarVezes(MARIA, 3);

        assertThatCode(() -> limitador.registrarPergunta(JOAO)).doesNotThrowAnyException();
    }

    private void registrarVezes(Long usuarioId, int vezes) {
        for (int i = 0; i < vezes; i++) {
            limitador.registrarPergunta(usuarioId);
        }
    }

    private void registrarEspacadoVezes(Long usuarioId, int vezes) {
        for (int i = 0; i < vezes; i++) {
            limitador.registrarPergunta(usuarioId);
            relogio.avancar(Duration.ofSeconds(61));
        }
    }
}
