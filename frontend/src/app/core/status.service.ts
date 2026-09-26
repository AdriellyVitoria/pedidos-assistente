import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable, retry, timeout } from 'rxjs';

import { API } from './modelos';

const TEMPO_MAXIMO_POR_TENTATIVA_MS = 20_000;
const INTERVALO_ENTRE_TENTATIVAS_MS = 5_000;
const TENTATIVAS = 24;

@Injectable({ providedIn: 'root' })
export class StatusService {
  private readonly http = inject(HttpClient);

  aguardarServidor(): Observable<void> {
    return this.http.get(`${API}/status`).pipe(
      timeout(TEMPO_MAXIMO_POR_TENTATIVA_MS),
      retry({ count: TENTATIVAS, delay: INTERVALO_ENTRE_TENTATIVAS_MS }),
      map(() => undefined),
    );
  }
}
