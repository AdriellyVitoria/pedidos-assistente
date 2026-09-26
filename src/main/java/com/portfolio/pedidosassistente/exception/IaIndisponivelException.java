package com.portfolio.pedidosassistente.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class IaIndisponivelException extends RuntimeException {

    private final HttpStatus status;

    public IaIndisponivelException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }
}
