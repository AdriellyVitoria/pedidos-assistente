import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { mensagemDeErro } from '../../core/erro-api';
import { StatusService } from '../../core/status.service';

const TEMPO_ATE_MOSTRAR_AVISO_MS = 2_000;

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly contasDemo = [
    { nome: 'Maria (cliente)', email: 'maria@email.com' },
    { nome: 'João (cliente)', email: 'joao@email.com' },
  ];

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });

  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mostrarSenha = signal(false);
  protected readonly servidor = signal<'verificando' | 'acordando' | 'pronto' | 'indisponivel'>('verificando');

  constructor() {
    const aviso = setTimeout(() => {
      if (this.servidor() === 'verificando') {
        this.servidor.set('acordando');
      }
    }, TEMPO_ATE_MOSTRAR_AVISO_MS);

    inject(StatusService)
      .aguardarServidor()
      .pipe(
        finalize(() => clearTimeout(aviso)),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: () => this.servidor.set('pronto'),
        error: () => this.servidor.set('indisponivel'),
      });
  }

  protected usarContaDemo(email: string): void {
    this.form.setValue({ email, senha: 'senha123' });
    this.erro.set(null);
  }

  protected entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);
    const { email, senha } = this.form.getRawValue();

    this.auth
      .login(email, senha)
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: () => this.router.navigate(['/chat']),
        error: (erro) => this.erro.set(mensagemDeErro(erro)),
      });
  }
}
