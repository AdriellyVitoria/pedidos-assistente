import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  const auth = { obterToken: vi.fn(), logout: vi.fn() };
  let http: HttpClient;
  let controlador: HttpTestingController;

  beforeEach(() => {
    auth.obterToken.mockReset().mockReturnValue('token-da-maria');
    auth.logout.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    controlador = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controlador.verify());

  it('envia o token para a nossa API', () => {
    http.get('/api/pedidos').subscribe();

    const requisicao = controlador.expectOne('/api/pedidos');
    expect(requisicao.request.headers.get('Authorization')).toBe('Bearer token-da-maria');
    requisicao.flush([]);
  });

  it('não envia o token para outros endereços', () => {
    http.get('https://outro-site.com/dados').subscribe();

    const requisicao = controlador.expectOne('https://outro-site.com/dados');
    expect(requisicao.request.headers.has('Authorization')).toBe(false);
    requisicao.flush({});
  });

  it('não envia cabeçalho quando não há token válido', () => {
    auth.obterToken.mockReturnValue(null);
    http.get('/api/pedidos').subscribe();

    const requisicao = controlador.expectOne('/api/pedidos');
    expect(requisicao.request.headers.has('Authorization')).toBe(false);
    requisicao.flush([]);
  });

  it('faz logout quando a API responde 401', () => {
    http.get('/api/pedidos').subscribe({ error: () => undefined });

    controlador.expectOne('/api/pedidos').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.logout).toHaveBeenCalled();
  });

  it('não faz logout quando o próprio login falha', () => {
    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });

    controlador.expectOne('/api/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.logout).not.toHaveBeenCalled();
  });
});
