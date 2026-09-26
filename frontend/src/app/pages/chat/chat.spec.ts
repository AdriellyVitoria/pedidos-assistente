import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { ChatService } from '../../core/chat.service';
import { PedidoService } from '../../core/pedido.service';
import { Chat } from './chat';

describe('Chat', () => {
  const chatService = { perguntar: vi.fn(), historico: vi.fn() };
  const pedidoService = { listarMeusPedidos: vi.fn() };
  const auth = { usuario: signal({ email: 'maria@email.com' }), logout: vi.fn() };
  let fixture: ComponentFixture<Chat>;

  beforeEach(async () => {
    HTMLElement.prototype.scrollTo = vi.fn();
    chatService.historico.mockReturnValue(of([]));
    pedidoService.listarMeusPedidos.mockReturnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [Chat],
      providers: [
        { provide: ChatService, useValue: chatService },
        { provide: PedidoService, useValue: pedidoService },
        { provide: AuthService, useValue: auth },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Chat);
    await fixture.whenStable();
  });

  async function receberResposta(resposta: string): Promise<HTMLElement> {
    chatService.perguntar.mockReturnValue(of({ resposta }));
    (fixture.componentInstance as unknown as { enviar(texto: string): void }).enviar('status do pedido 1?');
    await fixture.whenStable();
    const baloes = fixture.nativeElement.querySelectorAll('.balao.markdown') as NodeListOf<HTMLElement>;
    return baloes[baloes.length - 1];
  }

  it('remove HTML perigoso vindo da IA e mantém a formatação', async () => {
    const balao = await receberResposta(
      'Seu pedido está **Pago**. <img src="x" onerror="alert(1)"> <script>alert(2)</script>' +
        '<a href="javascript:alert(3)">clique</a>',
    );

    expect(balao.querySelector('strong')?.textContent).toBe('Pago');
    expect(balao.querySelector('script')).toBeNull();
    expect(balao.querySelector('img')?.hasAttribute('onerror') ?? false).toBe(false);
    expect(balao.querySelector('a')?.getAttribute('href') ?? '').toMatch(/^unsafe:/);
    expect(balao.innerHTML).not.toContain('alert(1)');
  });
});
