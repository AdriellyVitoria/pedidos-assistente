import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API, Pedido } from './modelos';

@Injectable({ providedIn: 'root' })
export class PedidoService {
  private readonly http = inject(HttpClient);

  listarMeusPedidos(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${API}/pedidos`);
  }
}
