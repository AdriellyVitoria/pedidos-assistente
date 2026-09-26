package com.portfolio.pedidosassistente.repository;

import com.portfolio.pedidosassistente.model.MensagemChat;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {

    List<MensagemChat> findByUsuarioIdAndTimestampAfterOrderByTimestampDesc(
            Long usuarioId, LocalDateTime desde, Limit limite);
}
