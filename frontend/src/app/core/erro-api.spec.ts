import { HttpErrorResponse } from '@angular/common/http';

import { mensagemDeErro } from './erro-api';

describe('mensagemDeErro', () => {
  it('usa o detail do ProblemDetail vindo do backend', () => {
    const erro = new HttpErrorResponse({
      status: 429,
      error: { detail: 'Você está enviando perguntas muito rápido.' },
    });

    expect(mensagemDeErro(erro)).toBe('Você está enviando perguntas muito rápido.');
  });

  it('avisa quando o servidor está fora do ar', () => {
    expect(mensagemDeErro(new HttpErrorResponse({ status: 0 }))).toContain('Não foi possível conectar');
    expect(mensagemDeErro(new HttpErrorResponse({ status: 504 }))).toContain('Não foi possível conectar');
  });

  it('usa uma mensagem genérica quando não há detalhe', () => {
    expect(mensagemDeErro(new HttpErrorResponse({ status: 500 }))).toBe(
      'Ocorreu um erro inesperado. Tente novamente.',
    );
    expect(mensagemDeErro(new Error('qualquer coisa'))).toBe(
      'Ocorreu um erro inesperado. Tente novamente.',
    );
  });
});
