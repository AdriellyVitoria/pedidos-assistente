import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TextFieldModule } from '@angular/cdk/text-field';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { finalize } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { ChatService } from '../../core/chat.service';
import { mensagemDeErro } from '../../core/erro-api';
import { MarkdownPipe } from '../../core/markdown.pipe';
import { MensagemChat, Pedido, ROTULO_STATUS } from '../../core/modelos';
import { PedidoService } from '../../core/pedido.service';

@Component({
  selector: 'app-chat',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    TextFieldModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    MatTooltipModule,
    MarkdownPipe,
  ],
  templateUrl: './chat.html',
  styleUrl: './chat.scss',
})
export class Chat {
  protected readonly auth = inject(AuthService);
  private readonly chatService = inject(ChatService);
  private readonly pedidoService = inject(PedidoService);

  protected readonly rotuloStatus = ROTULO_STATUS;
  protected readonly sugestoes = [
    'Quais são os meus pedidos?',
    'Cadê meu pedido mais recente?',
    'Qual o status do pedido 1?',
  ];

  protected readonly mensagens = signal<MensagemChat[]>([]);
  protected readonly enviando = signal(false);
  protected readonly pedidos = signal<Pedido[]>([]);
  protected readonly carregandoPedidos = signal(false);
  protected readonly erroPedidos = signal<string | null>(null);

  protected readonly pergunta = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(500)],
  });

  private readonly listaMensagens = viewChild<ElementRef<HTMLElement>>('listaMensagens');

  constructor() {
    this.carregarPedidos();
  }

  protected carregarPedidos(): void {
    this.carregandoPedidos.set(true);
    this.erroPedidos.set(null);
    this.pedidoService
      .listarMeusPedidos()
      .pipe(finalize(() => this.carregandoPedidos.set(false)))
      .subscribe({
        next: (pedidos) => this.pedidos.set(pedidos),
        error: (erro) => this.erroPedidos.set(mensagemDeErro(erro)),
      });
  }

  protected enviar(texto: string = this.pergunta.value): void {
    const pergunta = texto.trim();
    if (!pergunta || this.enviando()) {
      return;
    }

    this.mensagens.update((lista) => [...lista, { autor: 'usuario', texto: pergunta }]);
    this.pergunta.reset();
    this.enviando.set(true);
    this.rolarParaFim();

    this.chatService
      .perguntar(pergunta)
      .pipe(
        finalize(() => {
          this.enviando.set(false);
          this.rolarParaFim();
        }),
      )
      .subscribe({
        next: (resposta) =>
          this.mensagens.update((lista) => [...lista, { autor: 'assistente', texto: resposta.resposta }]),
        error: (erro) =>
          this.mensagens.update((lista) => [
            ...lista,
            { autor: 'assistente', texto: mensagemDeErro(erro), erro: true },
          ]),
      });
  }

  protected aoPressionarEnter(evento: Event): void {
    if (!(evento as KeyboardEvent).shiftKey) {
      evento.preventDefault();
      this.enviar();
    }
  }

  private rolarParaFim(): void {
    setTimeout(() => {
      const elemento = this.listaMensagens()?.nativeElement;
      elemento?.scrollTo({ top: elemento.scrollHeight, behavior: 'smooth' });
    });
  }
}
