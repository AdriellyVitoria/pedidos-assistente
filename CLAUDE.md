# Contexto do Projeto — Assistente de Pedidos com IA

## O que é este projeto

Sistema onde um usuário autenticado conversa com um assistente de IA para tirar dúvidas sobre os **próprios pedidos** (ex: "cadê meu pedido de ontem?", "qual o status da entrega X?"). O assistente responde apenas com dados reais do banco, nunca inventa informação, e nunca acessa dados de outro usuário.

Este projeto tem dois objetivos:
1. Servir como projeto de portfólio para vaga júnior/pleno Java + Angular.
2. Demonstrar na prática um padrão seguro de integração de LLM com dados sensíveis (function calling, nunca SQL livre pra IA).

## Stack

- **Backend:** Java 21, Spring Boot, Spring Data JPA, Spring Security (JWT), PostgreSQL
- **Frontend:** Angular (Reactive Forms, Angular Material ou PrimeNG)
- **IA:** Google Gemini (camada gratuita do AI Studio), acessado pelo endpoint compatível com o formato da OpenAI, usando function calling (nunca geração de SQL livre)
  - O backend tem um único cliente HTTP no formato OpenAI; provedor, chave e modelo vêm de variáveis de ambiente (`IA_BASE_URL`, `IA_API_KEY`, `IA_MODELO`), então trocar de provedor (Ollama, Groq, OpenAI) não exige mudar código
  - A chave nunca vai para o código ou para o git, só para variável de ambiente
  - Testes automatizados usam uma IA simulada (mock), sem chamar a API real

## Arquitetura (visão geral)

```
Usuário → Frontend Angular (login + chat)
              ↓ HTTP (JWT no header)
        Backend Spring Boot
              ↓
   Spring Security valida o token e extrai o usuarioId
              ↓
   Endpoint /chat recebe a pergunta
              ↓
   IA decide qual função chamar (ex: getStatusPedido)
              ↓
   Backend valida se o pedido pertence ao usuarioId do token
              ↓
   Consulta real no banco (Postgres)
              ↓
   Resultado volta pra IA → IA formata resposta em linguagem natural
              ↓
   Frontend exibe a resposta no chat
```

## Regras de segurança inegociáveis

- A IA **nunca** gera ou executa SQL diretamente. Ela só chama funções pré-definidas no backend.
- O `usuarioId` usado em qualquer consulta **sempre** vem do JWT decodificado no backend, nunca de um parâmetro enviado pelo cliente ou decidido pela IA.
- Toda função chamada pela IA deve validar, no backend, que o recurso pedido pertence ao usuário autenticado antes de retornar qualquer dado.
- A IA deve responder apenas com base no que as funções retornam. Se não houver dado, ela deve dizer que não encontrou — nunca inventar status, valores ou prazos.

## Entidades principais

- **Usuario**: id, nome, email, senha (hash), role (CLIENTE/ADMIN)
- **Pedido**: id, usuario_id (FK), status, data_criacao, valor_total
- **ItemPedido**: id, pedido_id (FK), nome_produto, quantidade, preco_unitario
- **MensagemChat**: id, usuario_id, pergunta, resposta, timestamp (histórico/auditoria)

## Fases do projeto (ordem de execução)

1. Modelagem do banco (entidades JPA + relacionamentos)
2. Backend CRUD básico de pedidos, sem IA ainda (endpoints simples pra validar a base)
3. Autenticação com Spring Security + JWT
4. Integração com IA via function calling (endpoint /chat)
5. Frontend Angular (login + tela de chat)
6. Refino do comportamento da IA (evitar alucinação, escopo restrito a pedidos)
7. Testes automatizados (JUnit + Mockito) focados em segurança e regras de negócio
8. Deploy (Docker + hospedagem gratuita) e documentação (README com prints, arquitetura e "o que deu errado no caminho")

## Estado atual do projeto

> Atualize esta seção conforme for avançando, para o Claude sempre saber onde você parou.

- [x] Fase 1 — Modelagem do banco (entidades em `model/`, Postgres via `docker compose` na porta 5433)
- [x] Fase 2 — Backend CRUD básico (app na porta 8081, dados de exemplo no perfil `dev`)
- [x] Fase 3 — Autenticação JWT (`POST /auth/login`, validação via `oauth2-resource-server`, HS256)
- [ ] Fase 4 — Integração com IA
- [ ] Fase 5 — Frontend Angular
- [ ] Fase 6 — Refino da IA
- [ ] Fase 7 — Testes
- [ ] Fase 8 — Deploy e documentação

## Como o Claude deve me ajudar

- Não pular etapas de segurança (autenticação, validação de posse do recurso) mesmo que pareça "mais rápido" sem elas.
- Priorizar simplicidade sobre arquitetura complexa — este é um projeto de nível júnior/pleno, não deve usar padrões como hexagonal architecture completa ou microsserviços.
- Sempre explicar o porquê de uma decisão técnica, não só entregar o código pronto, porque preciso conseguir explicar isso em entrevista técnica.
- Ao terminar uma fase, sugerir como testar antes de avançar para a próxima.
