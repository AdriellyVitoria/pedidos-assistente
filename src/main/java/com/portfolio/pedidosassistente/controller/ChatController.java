package com.portfolio.pedidosassistente.controller;

import com.portfolio.pedidosassistente.dto.ChatRequest;
import com.portfolio.pedidosassistente.dto.ChatResponse;
import com.portfolio.pedidosassistente.dto.MensagemHistoricoResponse;
import com.portfolio.pedidosassistente.security.UsuarioAutenticado;
import com.portfolio.pedidosassistente.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse perguntar(@RequestBody @Valid ChatRequest request,
                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return chatService.perguntar(request.pergunta(), usuario.id());
    }

    @GetMapping("/historico")
    public List<MensagemHistoricoResponse> historico(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return chatService.listarHistorico(usuario.id());
    }
}
