import { HttpErrorResponse } from '@angular/common/http';

export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0 || erro.status === 502 || erro.status === 504) {
      return 'Não foi possível conectar ao servidor. Tente novamente em instantes.';
    }
    const detalhe = erro.error?.detail;
    if (typeof detalhe === 'string' && detalhe.length > 0) {
      return detalhe;
    }
  }
  return 'Ocorreu um erro inesperado. Tente novamente.';
}
