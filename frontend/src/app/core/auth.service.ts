import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { map, Observable, tap } from 'rxjs';

import { API, LoginResponse } from './modelos';

const CHAVE_TOKEN = 'pedidos-assistente.token';

interface PayloadToken {
  sub: string;
  email: string;
  role: string;
  exp: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly token = signal<string | null>(sessionStorage.getItem(CHAVE_TOKEN));

  readonly usuario = computed(() => {
    const token = this.token();
    return token ? decodificarPayload(token) : null;
  });

  login(email: string, senha: string): Observable<void> {
    return this.http.post<LoginResponse>(`${API}/auth/login`, { email, senha }).pipe(
      tap((resposta) => {
        sessionStorage.setItem(CHAVE_TOKEN, resposta.token);
        this.token.set(resposta.token);
      }),
      map(() => undefined),
    );
  }

  estaAutenticado(): boolean {
    const usuario = this.usuario();
    return usuario !== null && usuario.exp * 1000 > Date.now();
  }

  obterToken(): string | null {
    return this.estaAutenticado() ? this.token() : null;
  }

  logout(): void {
    sessionStorage.removeItem(CHAVE_TOKEN);
    this.token.set(null);
    this.router.navigate(['/login']);
  }
}

function decodificarPayload(token: string): PayloadToken | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(base64), (caractere) => caractere.charCodeAt(0));
    return JSON.parse(new TextDecoder().decode(bytes)) as PayloadToken;
  } catch {
    return null;
  }
}
