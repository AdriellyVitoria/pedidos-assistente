import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject, throwError } from 'rxjs';

import { StatusService } from '../../core/status.service';
import { Login } from './login';

describe('Login - aviso de servidor acordando', () => {
  const statusService = { aguardarServidor: vi.fn() };
  let fixture: ComponentFixture<Login>;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), provideHttpClient(), { provide: StatusService, useValue: statusService }],
    });
  });

  afterEach(() => vi.useRealTimers());

  function textoDoAviso(): string {
    fixture.detectChanges();
    return (fixture.nativeElement.querySelector('.aviso') as HTMLElement | null)?.textContent ?? '';
  }

  it('não mostra aviso quando o servidor responde rápido', () => {
    const resposta = new Subject<void>();
    statusService.aguardarServidor.mockReturnValue(resposta);
    fixture = TestBed.createComponent(Login);

    resposta.next();
    resposta.complete();
    vi.advanceTimersByTime(3_000);

    expect(textoDoAviso()).toBe('');
  });

  it('mostra "acordando o servidor" após 2 segundos e esconde quando ele responde', () => {
    const resposta = new Subject<void>();
    statusService.aguardarServidor.mockReturnValue(resposta);
    fixture = TestBed.createComponent(Login);

    vi.advanceTimersByTime(1_900);
    expect(textoDoAviso()).toBe('');

    vi.advanceTimersByTime(200);
    expect(textoDoAviso()).toContain('Acordando o servidor');

    resposta.next();
    resposta.complete();
    expect(textoDoAviso()).toBe('');
  });

  it('avisa quando o servidor continua indisponível depois das tentativas', () => {
    statusService.aguardarServidor.mockReturnValue(throwError(() => new Error('fora do ar')));
    fixture = TestBed.createComponent(Login);

    expect(textoDoAviso()).toContain('indisponível');
  });
});
