export const API = '/api';

export interface LoginResponse {
  token: string;
  tipo: string;
  expiraEmSegundos: number;
}

export type StatusPedido =
  | 'AGUARDANDO_PAGAMENTO'
  | 'PAGO'
  | 'EM_SEPARACAO'
  | 'ENVIADO'
  | 'ENTREGUE'
  | 'CANCELADO';

export const ROTULO_STATUS: Record<StatusPedido, string> = {
  AGUARDANDO_PAGAMENTO: 'Aguardando pagamento',
  PAGO: 'Pago',
  EM_SEPARACAO: 'Em separação',
  ENVIADO: 'Enviado',
  ENTREGUE: 'Entregue',
  CANCELADO: 'Cancelado',
};

export interface ItemPedido {
  nomeProduto: string;
  quantidade: number;
  precoUnitario: number;
  subtotal: number;
}

export interface Pedido {
  id: number;
  status: StatusPedido;
  dataCriacao: string;
  valorTotal: number;
  itens: ItemPedido[];
}

export interface ChatResponse {
  resposta: string;
}

export interface MensagemChat {
  autor: 'usuario' | 'assistente';
  texto: string;
  erro?: boolean;
}
