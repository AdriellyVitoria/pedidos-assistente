package com.portfolio.pedidosassistente.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtParaUsuarioConverterTest {

    private final JwtParaUsuarioConverter converter = new JwtParaUsuarioConverter();

    @Test
    void montaOUsuarioAutenticadoAPartirDoToken() {
        AbstractAuthenticationToken autenticacao = converter.convert(jwt("7", "maria@email.com", "CLIENTE"));

        assertThat(autenticacao.isAuthenticated()).isTrue();
        assertThat(autenticacao.getPrincipal()).isInstanceOfSatisfying(UsuarioAutenticado.class, usuario -> {
            assertThat(usuario.id()).isEqualTo(7L);
            assertThat(usuario.email()).isEqualTo("maria@email.com");
            assertThat(usuario.senha()).isNull();
        });
        assertThat(autenticacao.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CLIENTE");
    }

    @Test
    void roleDoTokenViraAutoridadeComPrefixoRole() {
        AbstractAuthenticationToken autenticacao = converter.convert(jwt("1", "admin@email.com", "ADMIN"));

        assertThat(autenticacao.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    private Jwt jwt(String id, String email, String role) {
        Instant agora = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(id)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(60))
                .build();
    }
}
