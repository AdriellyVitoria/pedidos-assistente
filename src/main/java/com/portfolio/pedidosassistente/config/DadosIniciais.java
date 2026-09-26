package com.portfolio.pedidosassistente.config;

import com.portfolio.pedidosassistente.model.ItemPedido;
import com.portfolio.pedidosassistente.model.Pedido;
import com.portfolio.pedidosassistente.model.Role;
import com.portfolio.pedidosassistente.model.StatusPedido;
import com.portfolio.pedidosassistente.model.Usuario;
import com.portfolio.pedidosassistente.repository.PedidoRepository;
import com.portfolio.pedidosassistente.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.dados-demo.habilitado", havingValue = "true")
@RequiredArgsConstructor
public class DadosIniciais implements CommandLineRunner {

    private static final String SENHA_CLIENTES_DEMO = "senha123";

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock relogio;

    @Value("${app.dados-demo.senha-admin}")
    private String senhaAdmin;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now(relogio);
        Usuario maria = criarUsuario("Maria Souza", "maria@email.com", SENHA_CLIENTES_DEMO, Role.CLIENTE);
        Usuario joao = criarUsuario("João Lima", "joao@email.com", SENHA_CLIENTES_DEMO, Role.CLIENTE);

        criarPedido(maria, StatusPedido.ENVIADO, agora.minusDays(1),
                new ItemPedido("Fone de ouvido Bluetooth", 1, new BigDecimal("199.90")),
                new ItemPedido("Capa para celular", 2, new BigDecimal("29.90")));
        criarPedido(maria, StatusPedido.ENTREGUE, agora.minusDays(15),
                new ItemPedido("Livro Clean Code", 1, new BigDecimal("89.00")));
        criarPedido(joao, StatusPedido.AGUARDANDO_PAGAMENTO, agora.minusHours(3),
                new ItemPedido("Teclado mecânico", 1, new BigDecimal("349.90")));

        if (StringUtils.hasText(senhaAdmin)) {
            criarUsuario("Admin", "admin@email.com", senhaAdmin, Role.ADMIN);
        }
        log.info("Dados de demonstração criados");
    }

    private Usuario criarUsuario(String nome, String email, String senha, Role role) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setRole(role);
        return usuarioRepository.save(usuario);
    }

    private void criarPedido(Usuario usuario, StatusPedido status, LocalDateTime data, ItemPedido... itens) {
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setStatus(status);
        pedido.setDataCriacao(data);
        for (ItemPedido item : itens) {
            pedido.adicionarItem(item);
        }
        pedidoRepository.save(pedido);
    }
}
