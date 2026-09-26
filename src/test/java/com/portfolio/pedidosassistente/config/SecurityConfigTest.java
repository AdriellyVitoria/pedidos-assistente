package com.portfolio.pedidosassistente.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void recusaChaveJwtComMenosDe32Bytes() {
        assertThatThrownBy(() -> securityConfig.jwtSecretKey("chave-curta"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void aceitaChaveJwtCom32BytesOuMais() {
        assertThat(securityConfig.jwtSecretKey("12345678901234567890123456789012").getAlgorithm())
                .isEqualTo("HmacSHA256");
    }
}
