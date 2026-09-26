package com.portfolio.pedidosassistente.repository;

import com.portfolio.pedidosassistente.model.MensagemChat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {
}
