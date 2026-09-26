package com.portfolio.pedidosassistente.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

class RelogioAjustavel extends Clock {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Instant agora;

    RelogioAjustavel(Instant inicio) {
        this.agora = inicio;
    }

    void avancar(Duration duracao) {
        agora = agora.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return FUSO;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return Clock.fixed(agora, zona);
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
