import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { AuthService } from './auth.service';

const CHAVE_TOKEN = 'pedidos-assistente.token';

function base64Url(texto: string): string {
  return btoa(texto).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function criarToken(expiraEmSegundos: number): string {
  const payload = {
    sub: '1',
    email: 'maria@email.com',
    role: 'CLIENTE',
    exp: Math.floor(Date.now() / 1000) + expiraEmSegundos,
  };
  return `${base64Url('{"alg":"HS256"}')}.${base64Url(JSON.stringify(payload))}.assinatura`;
}

describe('AuthService', () => {
  let http: HttpTestingController;

  function criarServico(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => sessionStorage.clear());
  afterEach(() => http.verify());

  it('guarda o token e expõe o usuário depois do login', () => {
    const servico = criarServico();
    const token = criarToken(3600);

    servico.login('maria@email.com', 'senha123').subscribe();
    const requisicao = http.expectOne('/api/auth/login');
    expect(requisicao.request.method).toBe('POST');
    expect(requisicao.request.body).toEqual({ email: 'maria@email.com', senha: 'senha123' });
    requisicao.flush({ token, tipo: 'Bearer', expiraEmSegundos: 3600 });

    expect(servico.estaAutenticado()).toBe(true);
    expect(servico.usuario()?.email).toBe('maria@email.com');
    expect(servico.obterToken()).toBe(token);
    expect(sessionStorage.getItem(CHAVE_TOKEN)).toBe(token);
  });

  it('considera um token expirado como não autenticado', () => {
    sessionStorage.setItem(CHAVE_TOKEN, criarToken(-10));
    const servico = criarServico();

    expect(servico.estaAutenticado()).toBe(false);
    expect(servico.obterToken()).toBeNull();
  });

  it('ignora um token malformado', () => {
    sessionStorage.setItem(CHAVE_TOKEN, 'isso-nao-e-um-jwt');
    const servico = criarServico();

    expect(servico.usuario()).toBeNull();
    expect(servico.estaAutenticado()).toBe(false);
  });

  it('logout apaga o token e volta para o login', () => {
    sessionStorage.setItem(CHAVE_TOKEN, criarToken(3600));
    const servico = criarServico();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    servico.logout();

    expect(servico.estaAutenticado()).toBe(false);
    expect(sessionStorage.getItem(CHAVE_TOKEN)).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/login']);
  });
});
