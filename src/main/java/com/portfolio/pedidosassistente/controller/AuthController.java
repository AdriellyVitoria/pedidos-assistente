package com.portfolio.pedidosassistente.controller;

import com.portfolio.pedidosassistente.dto.LoginRequest;
import com.portfolio.pedidosassistente.dto.LoginResponse;
import com.portfolio.pedidosassistente.security.TokenService;
import com.portfolio.pedidosassistente.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        Authentication autenticacao = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.senha()));
        return tokenService.gerarToken((UsuarioAutenticado) autenticacao.getPrincipal());
    }
}
