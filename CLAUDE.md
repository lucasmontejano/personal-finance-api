# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Sobre o projeto

API REST pra controle de finanças pessoais. Java 21, Spring Boot 3.4.2, Postgres 16, Flyway, JWT stateless. Pacote `com.lucas.financas`.

## Comandos

```bash
docker compose up -d            # postgres na porta 5433
mvn spring-boot:run             # app na porta 8080
.\smoke-test.ps1                # smoke test fim-a-fim contra a app rodando
```

Maven não tá no PATH do sistema — o que vem com IntelliJ está em `C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd`. Pode rodar pela IDE, chamar com path completo, ou gerar wrapper com `mvn -N wrapper:wrapper`.

**Validação por fase usa o smoke-test.** Preferência forte do dev: NÃO testar à mão pelo IntelliJ HTTP client (clicar request por request). Estender `smoke-test.ps1` com cada feature nova e rodar pra verificar.

## Arquitetura

Camadas Spring convencionais (`controller → service → repository → entity JPA`). DTOs como records.

### Decisões importantes (não óbvias do código)

**Saldo é calculado, não armazenado.** `ContaService.calcularSaldo()` soma `saldoInicial + receitas - despesas + transferências recebidas - transferências enviadas` toda vez. Não tentar cachear na entidade `Conta`.

**Soft delete em `Conta` (flag `ativa`) e `Transacao` (flag `deletado`).** Listagens filtram, mas histórico de transações deletadas continua válido pra cálculos.

**Idempotency-Key header em `POST /transacoes`.** Mesma key + mesmo usuário devolve a transação existente. Recorrências usam key automática `rec_<id>_<yyyy-MM>` — chamar `executar-agora` várias vezes no mesmo mês não duplica.

**Validação Tipo × Categoria.** RECEITA/DESPESA exigem `categoriaId` cuja `tipo` bate. TRANSFERENCIA exige `contaDestinoId` e proíbe `categoriaId`. Lógica em `TransacaoService.validar()` está duplicada em `TransacaoRecorrenteService.validar()` **de propósito**, pra não acoplar os services. Se mudar uma, mudar a outra.

**Multi-tenancy por query.** Toda repository tem `findBy...AndUsuarioId(...)`. Nunca buscar/retornar dados sem esse filtro — regra é "achar ou 404", não "achar e dar 403".

**Categorias seedadas no cadastro.** `UsuarioService.cadastrar()` insere as 13 categorias padrão do usuário. Cada usuário tem suas próprias — não compartilhadas.

**Recorrências = scheduler + endpoint manual.** `@Scheduled(cron = "0 0 1 * * *")` em `TransacoesRecorrentesScheduler` processa diariamente. `POST /transacoes-recorrentes/{id}/executar-agora` dispara manualmente. Ambos roteiam por `TransacaoService.criar()` com a idempotency key.

**JWT stateless.** `JwtAuthFilter` antes do `UsernamePasswordAuthenticationFilter`, popula `SecurityContext` via `UserDetailsService`. Pegar usuário logado com `@AuthenticationPrincipal Usuario user`.

### Migrations Flyway

`src/main/resources/db/migration/V*__*.sql`. `ddl-auto: validate` — Hibernate só confere, não cria. **Sempre criar migration nova**, nunca editar uma já aplicada (Flyway compara checksum e quebra).

### Testes

Validação é via `smoke-test.ps1` (PowerShell + Invoke-WebRequest contra a app rodando). Não tem teste unitário/integração — projeto pessoal, smoke cobre o necessário. Estender o script a cada feature nova.

## Estilo

- **Português pt-BR** em código, comentários, mensagens de erro, migrations, READMEs. Ex: `Usuario`, `Conta`, `/contas`, `// pega o usuario logado`.
- Comentários **estilo dev júnior**: curtos, casuais, explicando o "porquê" rápido. Nada de Javadoc formal. Nem todo método precisa de comentário.
- **Minimizar cara de IA**: sem emojis no código, sem docstrings perfeitos, sem nomes longos demais, aceitar pequenas inconsistências naturais.
- **`BigDecimal` pra dinheiro**, nunca `double`/`float`.
- **Sem Lombok** (foi removido — bateu erro no IntelliJ). Records pra DTOs, getters/setters manuais nas entidades. Não adicionar de volta sem perguntar.
- Conversas/explicações comigo podem ser em inglês; artefatos (código, migrations, docs commitados) ficam em português.
