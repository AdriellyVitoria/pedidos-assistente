package com.portfolio.pedidosassistente.security;

import com.portfolio.pedidosassistente.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {

    public static final String ISSUER = "pedidos-assistente";

    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.expiracao-minutos}")
    private long expiracaoMinutos;

    public LoginResponse gerarToken(UsuarioAutenticado usuario) {
        Instant agora = Instant.now();
        Duration validade = Duration.ofMinutes(expiracaoMinutos);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(usuario.id().toString())
                .claim("email", usuario.email())
                .claim("role", usuario.role())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(token, "Bearer", validade.toSeconds());
    }
}
