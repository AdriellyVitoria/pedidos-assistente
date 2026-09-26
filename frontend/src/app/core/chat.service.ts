import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API, ChatResponse } from './modelos';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);

  perguntar(pergunta: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${API}/chat`, { pergunta });
  }
}
