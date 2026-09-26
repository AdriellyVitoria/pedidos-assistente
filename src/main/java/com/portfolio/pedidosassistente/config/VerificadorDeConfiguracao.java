package com.portfolio.pedidosassistente.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

public class VerificadorDeConfiguracao implements EnvironmentPostProcessor, Ordered {

    private static final Map<String, String> VARIAVEL_POR_PROPRIEDADE = Map.of(
            "spring.datasource.url", "DB_URL",
            "spring.datasource.username", "DB_USERNAME",
            "spring.datasource.password", "DB_PASSWORD",
            "app.jwt.secret", "JWT_SECRET",
            "app.ia.api-key", "IA_API_KEY"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment ambiente, SpringApplication aplicacao) {
        if (ambiente.acceptsProfiles(Profiles.of("dev"))) {
            return;
        }

        List<String> faltando = VARIAVEL_POR_PROPRIEDADE.entrySet().stream()
                .filter(entrada -> !estaConfigurada(ambiente, entrada.getKey()))
                .map(Map.Entry::getValue)
                .sorted()
                .toList();

        if (!faltando.isEmpty()) {
            throw new IllegalStateException("Configuração obrigatória ausente. Defina as variáveis de ambiente "
                    + String.join(", ", faltando)
                    + " ou rode com o perfil dev para usar os valores de desenvolvimento.");
        }
    }

    private boolean estaConfigurada(ConfigurableEnvironment ambiente, String propriedade) {
        try {
            String valor = ambiente.getProperty(propriedade);
            return StringUtils.hasText(valor) && !valor.contains("${");
        } catch (IllegalArgumentException placeholderSemValor) {
            return false;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
