package com.portfolio.pedidosassistente.controller;

import com.portfolio.pedidosassistente.dto.CriarPedidoRequest;
import com.portfolio.pedidosassistente.dto.PedidoResponse;
import com.portfolio.pedidosassistente.security.UsuarioAutenticado;
import com.portfolio.pedidosassistente.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public List<PedidoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pedidoService.listarDoUsuario(usuario.id());
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pedidoService.buscarDoUsuario(id, usuario.id());
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@RequestBody @Valid CriarPedidoRequest request,
                                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        PedidoResponse criado = pedidoService.criar(request, usuario.id());
        return ResponseEntity.created(URI.create("/pedidos/" + criado.id())).body(criado);
    }

    @PatchMapping("/{id}/cancelar")
    public PedidoResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return pedidoService.cancelar(id, usuario.id());
    }
}
