import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';
import { API } from './modelos';

export const authInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  const auth = inject(AuthService);
  const token = auth.obterToken();
  const ehNossaApi = requisicao.url.startsWith(API);

  const requisicaoFinal =
    token && ehNossaApi
      ? requisicao.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : requisicao;

  return proximo(requisicaoFinal).pipe(
    catchError((erro: unknown) => {
      const ehLogin = requisicao.url.endsWith('/auth/login');
      if (erro instanceof HttpErrorResponse && erro.status === 401 && ehNossaApi && !ehLogin) {
        auth.logout();
      }
      return throwError(() => erro);
    }),
  );
};
