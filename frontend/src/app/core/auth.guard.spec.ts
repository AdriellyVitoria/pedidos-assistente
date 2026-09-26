import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  provideRouter,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';

import { authGuard, visitanteGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('guards de autenticação', () => {
  const auth = { estaAutenticado: vi.fn() };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  });

  function executar(guard: CanActivateFn) {
    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  }

  it('authGuard libera usuário autenticado', () => {
    auth.estaAutenticado.mockReturnValue(true);

    expect(executar(authGuard)).toBe(true);
  });

  it('authGuard manda visitante para o login', () => {
    auth.estaAutenticado.mockReturnValue(false);

    const resultado = executar(authGuard) as UrlTree;

    expect(resultado.toString()).toBe('/login');
  });

  it('visitanteGuard manda usuário já logado para o chat', () => {
    auth.estaAutenticado.mockReturnValue(true);

    const resultado = executar(visitanteGuard) as UrlTree;

    expect(resultado.toString()).toBe('/chat');
  });
});
