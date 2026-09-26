package com.portfolio.pedidosassistente.config;

import com.portfolio.pedidosassistente.model.ItemPedido;
import com.portfolio.pedidosassistente.model.Pedido;
import com.portfolio.pedidosassistente.model.Role;
import com.portfolio.pedidosassistente.model.StatusPedido;
import com.portfolio.pedidosassistente.model.Usuario;
import com.portfolio.pedidosassistente.repository.PedidoRepository;
import com.portfolio.pedidosassistente.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DadosIniciais implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario maria = criarUsuario("Maria Souza", "maria@email.com", Role.CLIENTE);
        Usuario joao = criarUsuario("João Lima", "joao@email.com", Role.CLIENTE);
        criarUsuario("Admin", "admin@email.com", Role.ADMIN);

        criarPedido(maria, StatusPedido.ENVIADO, LocalDateTime.now().minusDays(1),
                new ItemPedido("Fone de ouvido Bluetooth", 1, new BigDecimal("199.90")),
                new ItemPedido("Capa para celular", 2, new BigDecimal("29.90")));
        criarPedido(maria, StatusPedido.ENTREGUE, LocalDateTime.now().minusDays(15),
                new ItemPedido("Livro Clean Code", 1, new BigDecimal("89.00")));
        criarPedido(joao, StatusPedido.AGUARDANDO_PAGAMENTO, LocalDateTime.now().minusHours(3),
                new ItemPedido("Teclado mecânico", 1, new BigDecimal("349.90")));
    }

    private Usuario criarUsuario(String nome, String email, Role role) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode("senha123"));
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
