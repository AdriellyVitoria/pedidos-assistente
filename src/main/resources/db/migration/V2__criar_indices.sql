CREATE INDEX idx_pedidos_usuario_data ON pedidos (usuario_id, data_criacao DESC);

CREATE INDEX idx_itens_pedido_pedido ON itens_pedido (pedido_id);

CREATE INDEX idx_mensagens_chat_usuario_timestamp ON mensagens_chat (usuario_id, "timestamp" DESC);
