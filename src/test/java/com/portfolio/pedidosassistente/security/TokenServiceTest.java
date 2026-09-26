package com.portfolio.pedidosassistente.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.portfolio.pedidosassistente.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private final SecretKey chave = new SecretKeySpec(
            "chave-de-teste-com-pelo-menos-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private TokenService tokenService;

    @BeforeEach
    void configurar() {
        tokenService = new TokenService(new NimbusJwtEncoder(new ImmutableSecret<>(chave)));
        ReflectionTestUtils.setField(tokenService, "expiracaoMinutos", 60L);
    }

    @Test
    void tokenContemAIdentidadeDoUsuarioEAExpiracaoConfigurada() {
        Instant antes = Instant.now();

        LoginResponse resposta = tokenService.gerarToken(new UsuarioAutenticado(7L, "maria@email.com", "hash", "CLIENTE"));

        Jwt jwt = decodificar(resposta.token());
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("maria@email.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CLIENTE");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(TokenService.ISSUER);
        assertThat(jwt.getExpiresAt()).isBetween(antes.plusSeconds(3595), Instant.now().plusSeconds(3605));
        assertThat(resposta.tipo()).isEqualTo("Bearer");
        assertThat(resposta.expiraEmSegundos()).isEqualTo(3600);
    }

    @Test
    void tokenNaoCarregaASenhaDoUsuario() {
        LoginResponse resposta = tokenService.gerarToken(
                new UsuarioAutenticado(7L, "maria@email.com", "hash-secreto", "CLIENTE"));

        Jwt jwt = decodificar(resposta.token());
        assertThat(jwt.getClaims()).containsOnlyKeys("iss", "sub", "email", "role", "iat", "exp");
        assertThat(jwt.getClaims().values()).doesNotContain("hash-secreto");
    }

    private Jwt decodificar(String token) {
        return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build().decode(token);
    }
}
