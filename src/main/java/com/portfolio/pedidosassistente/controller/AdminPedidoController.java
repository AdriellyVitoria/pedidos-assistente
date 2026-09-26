package com.portfolio.pedidosassistente.controller;

import com.portfolio.pedidosassistente.dto.AtualizarStatusRequest;
import com.portfolio.pedidosassistente.dto.PedidoResponse;
import com.portfolio.pedidosassistente.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/pedidos")
@RequiredArgsConstructor
public class AdminPedidoController {

    private final PedidoService pedidoService;

    @PatchMapping("/{id}/status")
    public PedidoResponse atualizarStatus(@PathVariable Long id, @RequestBody @Valid AtualizarStatusRequest request) {
        return pedidoService.atualizarStatus(id, request.status());
    }
}
