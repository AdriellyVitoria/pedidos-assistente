package com.portfolio.pedidosassistente.security;

import com.portfolio.pedidosassistente.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarioRepository.findByEmail(email)
                .map(UsuarioAutenticado::de)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }
}
