package com.portfolio.pedidosassistente.service;

import com.portfolio.pedidosassistente.config.ChatProperties;
import com.portfolio.pedidosassistente.exception.LimiteDePerguntasException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class LimitadorDePerguntas {

    private static final Duration UM_MINUTO = Duration.ofMinutes(1);

    private final ChatProperties propriedades;
    private final Clock relogio;
    private final Map<Long, UsoDoUsuario> usos = new ConcurrentHashMap<>();

    public void registrarPergunta(Long usuarioId) {
        UsoDoUsuario uso = usos.computeIfAbsent(usuarioId, id -> new UsoDoUsuario());

        synchronized (uso) {
            Instant agora = relogio.instant();
            LocalDate hoje = LocalDate.now(relogio);

            while (!uso.recentes.isEmpty() && !uso.recentes.peekFirst().isAfter(agora.minus(UM_MINUTO))) {
                uso.recentes.pollFirst();
            }
            if (!hoje.equals(uso.dia)) {
                uso.dia = hoje;
                uso.totalNoDia = 0;
            }

            if (uso.totalNoDia >= propriedades.limitePorDia()) {
                throw new LimiteDePerguntasException("Você atingiu o limite de " + propriedades.limitePorDia()
                        + " perguntas por dia. Tente novamente amanhã.");
            }
            if (uso.recentes.size() >= propriedades.limitePorMinuto()) {
                throw new LimiteDePerguntasException(
                        "Você está enviando perguntas muito rápido. Aguarde um minuto e tente novamente.");
            }

            uso.recentes.addLast(agora);
            uso.totalNoDia++;
        }
    }

    private static final class UsoDoUsuario {
        private final Deque<Instant> recentes = new ArrayDeque<>();
        private LocalDate dia;
        private int totalNoDia;
    }
}
