package com.portfolio.pedidosassistente.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtParaUsuarioConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                Long.valueOf(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                null,
                jwt.getClaimAsString("role")
        );
        return UsernamePasswordAuthenticationToken.authenticated(usuario, jwt, usuario.getAuthorities());
    }
}
