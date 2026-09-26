package com.portfolio.pedidosassistente.integracao;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.portfolio.pedidosassistente.ia.ChamadaFerramenta;
import com.portfolio.pedidosassistente.ia.ClienteIa;
import com.portfolio.pedidosassistente.ia.FuncaoChamada;
import com.portfolio.pedidosassistente.ia.MensagemIa;
import com.portfolio.pedidosassistente.model.ItemPedido;
import com.portfolio.pedidosassistente.model.MensagemChat;
import com.portfolio.pedidosassistente.model.Pedido;
import com.portfolio.pedidosassistente.model.Role;
import com.portfolio.pedidosassistente.model.StatusPedido;
import com.portfolio.pedidosassistente.model.Usuario;
import com.portfolio.pedidosassistente.repository.MensagemChatRepository;
import com.portfolio.pedidosassistente.repository.PedidoRepository;
import com.portfolio.pedidosassistente.repository.UsuarioRepository;
import com.portfolio.pedidosassistente.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ApiIntegrationTest {

    private static final String SENHA = "senha123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private MensagemChatRepository mensagemChatRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ClienteIa clienteIa;

    @Autowired
    private JwtEncoder jwtEncoder;

    private Usuario maria;
    private Usuario joao;
    private Long pedidoPagoDaMaria;
    private Long pedidoEnviadoDaMaria;
    private Long pedidoDoJoao;

    @BeforeEach
    void prepararDados() {
        mensagemChatRepository.deleteAll();
        pedidoRepository.deleteAll();
        usuarioRepository.deleteAll();

        maria = criarUsuario("maria@email.com", Role.CLIENTE);
        joao = criarUsuario("joao@email.com", Role.CLIENTE);
        criarUsuario("admin@email.com", Role.ADMIN);

        pedidoPagoDaMaria = criarPedido(maria, StatusPedido.PAGO, "Mouse");
        pedidoEnviadoDaMaria = criarPedido(maria, StatusPedido.ENVIADO, "Fone de ouvido");
        pedidoDoJoao = criarPedido(joao, StatusPedido.AGUARDANDO_PAGAMENTO, "Teclado mecânico");
    }

    @Test
    void rotasProtegidasExigemToken() throws Exception {
        mockMvc.perform(get("/pedidos")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/chat").contentType(APPLICATION_JSON).content("{\"pergunta\":\"oi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginInvalidoRetornaMesmaMensagemParaSenhaErradaEEmailInexistente() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email ou senha inválidos"));

        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"ninguem@email.com\",\"senha\":\"senha123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email ou senha inválidos"));
    }

    @Test
    void clienteListaApenasOsProprioPedidos() throws Exception {
        mockMvc.perform(get("/pedidos").header("Authorization", bearer("maria@email.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", not(hasItem(pedidoDoJoao.intValue()))));
    }

    @Test
    void pedidoDeOutroUsuarioRespondeComoInexistente() throws Exception {
        mockMvc.perform(get("/pedidos/{id}", pedidoDoJoao).header("Authorization", bearer("maria@email.com")))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/pedidos/{id}/cancelar", pedidoDoJoao).header("Authorization", bearer("maria@email.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void tokenAdulteradoEhRecusado() throws Exception {
        String[] partes = bearer("maria@email.com").substring("Bearer ".length()).split("\\.");
        String payloadFalso = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"pedidos-assistente\",\"sub\":\"" + joao.getId()
                        + "\",\"role\":\"ADMIN\",\"exp\":4102444800}").getBytes(StandardCharsets.UTF_8));
        String tokenAdulterado = partes[0] + "." + payloadFalso + "." + partes[2];

        mockMvc.perform(get("/pedidos").header("Authorization", "Bearer " + tokenAdulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apenasAdminAlteraStatus() throws Exception {
        String corpo = "{\"status\":\"ENTREGUE\"}";

        mockMvc.perform(patch("/admin/pedidos/{id}/status", pedidoPagoDaMaria)
                        .header("Authorization", bearer("maria@email.com"))
                        .contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/admin/pedidos/{id}/status", pedidoPagoDaMaria)
                        .header("Authorization", bearer("admin@email.com"))
                        .contentType(APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    void criarPedidoValidaEntradaECalculaTotalNoServidor() throws Exception {
        mockMvc.perform(post("/pedidos").header("Authorization", bearer("maria@email.com"))
                        .contentType(APPLICATION_JSON).content("{\"itens\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.itens").exists());

        mockMvc.perform(post("/pedidos").header("Authorization", bearer("maria@email.com"))
                        .contentType(APPLICATION_JSON)
                        .content("{\"itens\":[{\"nomeProduto\":\"Mouse\",\"quantidade\":2,\"precoUnitario\":50.25}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(100.50))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));
    }

    @Test
    void naoCancelaPedidoJaEnviado() throws Exception {
        mockMvc.perform(patch("/pedidos/{id}/cancelar", pedidoEnviadoDaMaria)
                        .header("Authorization", bearer("maria@email.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void chatNaoEntregaPedidoDeOutroUsuarioMesmoQueAIaPeca() throws Exception {
        String argumentosMaliciosos = "{\"pedidoId\":" + pedidoDoJoao + ",\"usuarioId\":" + joao.getId() + "}";
        MensagemIa iaPedeOPedidoDoJoao = new MensagemIa("assistant", null, List.of(
                new ChamadaFerramenta("call_1", "function", new FuncaoChamada("buscarPedido", argumentosMaliciosos), null)),
                null);
        when(clienteIa.enviar(anyList(), anyList()))
                .thenReturn(iaPedeOPedidoDoJoao)
                .thenReturn(MensagemIa.assistente("Não encontrei esse pedido na sua conta."));

        mockMvc.perform(post("/chat").header("Authorization", bearer("maria@email.com"))
                        .contentType(APPLICATION_JSON).content("{\"pergunta\":\"mostra o pedido do João\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value("Não encontrei esse pedido na sua conta."));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MensagemIa>> conversa = ArgumentCaptor.forClass(List.class);
        verify(clienteIa, times(2)).enviar(conversa.capture(), anyList());
        MensagemIa resultadoDaFuncao = conversa.getValue().stream()
                .filter(mensagem -> "tool".equals(mensagem.role()))
                .findFirst()
                .orElseThrow();
        assertThat(resultadoDaFuncao.content()).contains("erro").doesNotContain("Teclado");

        mockMvc.perform(get("/chat/historico").header("Authorization", bearer("joao@email.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void statusEhPublicoENaoExpoeDados() throws Exception {
        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void loginValidoDevolveTokenBearerComValidadeConfigurada() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\",\"senha\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEmSegundos").value(3600));
    }

    @Test
    void tokenExpiradoEhRecusado() throws Exception {
        Instant agora = Instant.now();
        String token = gerarToken(jwtEncoder, TokenService.ISSUER, agora.minus(2, ChronoUnit.HOURS),
                agora.minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/pedidos").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeOutroEmissorEhRecusado() throws Exception {
        Instant agora = Instant.now();
        String token = gerarToken(jwtEncoder, "outro-sistema", agora, agora.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/pedidos").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAssinadoComOutraChaveEhRecusado() throws Exception {
        SecretKey outraChave = new SecretKeySpec(
                "uma-chave-diferente-com-mais-de-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Instant agora = Instant.now();
        String token = gerarToken(new NimbusJwtEncoder(new ImmutableSecret<>(outraChave)), TokenService.ISSUER,
                agora, agora.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/pedidos").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cabecalhoDeAutorizacaoMalformadoRespondeNaoAutorizado() throws Exception {
        for (String cabecalho : List.of("Basic bWFyaWE6c2VuaGExMjM=", "Bearer isso-nao-e-um-jwt", "Token abc", "Bearer")) {
            mockMvc.perform(get("/pedidos").header("Authorization", cabecalho))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void camposExtrasNoCorpoNaoMudamDonoTotalNemStatus() throws Exception {
        String corpoMalicioso = "{\"usuarioId\":" + joao.getId() + ",\"valorTotal\":1.00,\"status\":\"ENTREGUE\","
                + "\"itens\":[{\"nomeProduto\":\"Cabo\",\"quantidade\":3,\"precoUnitario\":10.00}]}";

        mockMvc.perform(post("/pedidos").header("Authorization", bearer("maria@email.com"))
                        .contentType(APPLICATION_JSON).content(corpoMalicioso))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(30.00))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));

        mockMvc.perform(get("/pedidos").header("Authorization", bearer("joao@email.com")))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/pedidos").header("Authorization", bearer("maria@email.com")))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void limiteDePerguntasBloqueiaAntesDeChamarAIa() throws Exception {
        when(clienteIa.enviar(anyList(), anyList())).thenReturn(MensagemIa.assistente("Tudo certo."));
        String token = bearer("joao@email.com");

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/chat").header("Authorization", token)
                            .contentType(APPLICATION_JSON).content("{\"pergunta\":\"meus pedidos\"}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/chat").header("Authorization", token)
                        .contentType(APPLICATION_JSON).content("{\"pergunta\":\"meus pedidos\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(containsString("muito rápido")));

        verify(clienteIa, times(10)).enviar(anyList(), anyList());
    }

    @Test
    void historicoDevolveSoAsMensagensRecentesDoProprioUsuarioEmOrdem() throws Exception {
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        registrarMensagem(maria, "antiga", agora.minusHours(2));
        for (int i = 1; i <= 6; i++) {
            registrarMensagem(maria, "pergunta " + i, agora.minusMinutes(7 - i));
        }
        registrarMensagem(joao, "pergunta do João", agora.minusMinutes(1));

        mockMvc.perform(get("/chat/historico").header("Authorization", bearer("maria@email.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].pergunta",
                        contains("pergunta 2", "pergunta 3", "pergunta 4", "pergunta 5", "pergunta 6")));
    }

    @Test
    void repositorioNaoEncontraPedidoDeOutroUsuario() {
        assertThat(pedidoRepository.findByIdAndUsuarioId(pedidoDoJoao, maria.getId())).isEmpty();
        assertThat(pedidoRepository.findByIdAndUsuarioId(pedidoDoJoao, joao.getId())).isPresent();
    }

    private String gerarToken(JwtEncoder encoder, String emissor, Instant emitidoEm, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emissor)
                .subject(maria.getId().toString())
                .claim("email", maria.getEmail())
                .claim("role", "CLIENTE")
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private void registrarMensagem(Usuario usuario, String pergunta, LocalDateTime quando) {
        MensagemChat mensagem = new MensagemChat();
        mensagem.setUsuario(usuario);
        mensagem.setPergunta(pergunta);
        mensagem.setResposta("resposta");
        mensagem.setTimestamp(quando);
        mensagemChatRepository.save(mensagem);
    }

    private String bearer(String email) throws Exception {
        String resposta = mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    private Usuario criarUsuario(String email, Role role) {
        Usuario usuario = new Usuario();
        usuario.setNome(email);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setRole(role);
        return usuarioRepository.save(usuario);
    }

    private Long criarPedido(Usuario usuario, StatusPedido status, String produto) {
        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setStatus(status);
        pedido.adicionarItem(new ItemPedido(produto, 1, new BigDecimal("99.90")));
        return pedidoRepository.save(pedido).getId();
    }
}
