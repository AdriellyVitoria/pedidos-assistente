package com.portfolio.pedidosassistente.repository;

import com.portfolio.pedidosassistente.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = "itens")
    List<Pedido> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);

    @EntityGraph(attributePaths = "itens")
    Optional<Pedido> findByIdAndUsuarioId(Long id, Long usuarioId);
}
