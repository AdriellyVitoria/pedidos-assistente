package com.portfolio.pedidosassistente.security;

import com.portfolio.pedidosassistente.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record UsuarioAutenticado(Long id, String email, String senha, String role) implements UserDetails {

    public static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getEmail(), usuario.getSenha(), usuario.getRole().name());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
