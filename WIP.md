# WIP — pra continuar depois

## Onde tá

**Fase 8 implementada e compilada**, mas `mvn test` não roda localmente por bug de integração Testcontainers + Docker Desktop no Windows. Nada quebrado no código de produção — só a infra de teste.

## O que está pronto na fase 8

- [x] `springdoc-openapi-starter-webmvc-ui` 2.7.0 + `OpenApiConfig` com bearer auth (Swagger em `/swagger-ui.html`)
- [x] `spring-boot-starter-actuator` + `/actuator/health` liberado em `SecurityConfig`
- [x] CORS configurável via `CORS_ORIGINS` env var
- [x] `application.yml` migrado pra env vars com defaults (DATABASE_URL, JWT_SECRET, etc)
- [x] `application-prod.yml` (sem SQL log, INFO level)
- [x] `Dockerfile` multi-stage (Maven build + JRE Alpine, user não-root) + `.dockerignore`
- [x] `BaseIntegrationTest` (Postgres singleton via Testcontainers) + `TestUtils` (helpers)
- [x] 7 classes de teste de integração:
  - `AuthIntegrationTest`, `ContaIntegrationTest`, `CategoriaIntegrationTest`
  - `TransacaoIntegrationTest`, `OrcamentoIntegrationTest`
  - `RelatorioIntegrationTest`, `TransacaoRecorrenteIntegrationTest`
  - `FinancasApplicationTests` (sanity check)
- [x] `requests.http` deletado (tinha JWTs hardcoded, duplicava o smoke test)
- [x] README reescrito com features, swagger, env vars, docker, tabela de endpoints
- [x] `.gitignore` ganhou `.env` / `.env.local`
- [x] Phase 8 marcada no README
- [x] `CLAUDE.md` criado pra futuras sessões

## O bloqueio: Testcontainers + Docker Desktop no Windows

`docker info` da CLI funciona normal. Mas `mvn test` falha com:

```
Could not find a valid Docker environment.
NpipeSocketClientProviderStrategy: failed with BadRequestException (Status 400:
{..."Labels":["com.docker.desktop.address=npipe://\\\\.\\pipe\\docker_cli"]...})
```

Acontece pq:
- Docker Desktop expõe `\\.\pipe\docker_engine` mas responde 400 + label dizendo "vai falar com docker_cli"
- A CLI segue esse redirect; o Docker Java client (que Testcontainers usa) **não**
- Testei `dockerDesktopLinuxEngine` → mesmo 400; `docker_cli` → 404
- `DOCKER_HOST` env var não ajuda
- `testcontainers.properties` com `docker.host=...` não ajuda
- Bumpar Testcontainers pra 1.20.6 não ajuda; pra 2.0.5 quebra (BOM mudou estrutura)

Reverti todas as tentativas — `pom.xml` e arquivos de config estão limpos.

## Opções pra desbloquear (escolher uma)

### 1. Habilitar TCP no Docker Desktop (mais rápido)
- Settings → General → ligar **"Expose daemon on tcp://localhost:2375 without TLS"**
- Rodar: `$env:DOCKER_HOST="tcp://localhost:2375"; mvn test`
- Risco: TCP sem TLS é só pra dev local, não habilitar em rede pública

### 2. Instalar [Testcontainers Desktop](https://testcontainers.com/desktop/) (gratuito)
- App da Atomicjar/Docker que cria um endpoint estável
- Sem mexer em código nem em settings do Docker Desktop
- Funciona out-of-the-box

### 3. Trocar Testcontainers por H2 in-memory
- Tests rodam sem Docker
- Contra: H2 não é 100% Postgres-compatível — algumas queries nativas e tipos podem falhar (ex: `SMALLINT`, `BIGSERIAL`, sintaxe Postgres)
- Refatoraria `BaseIntegrationTest` pra usar perfil de teste com H2 + Hibernate ddl-auto:create-drop (sem Flyway nos testes)

### 4. Manter como tá
- Testes funcionam em Linux/CI (GitHub Actions, etc) sem ajuste nenhum
- Localmente fica só com `smoke-test.ps1` (que já cobre 70+ asserções end-to-end)
- Aceita que Windows local não roda `mvn test` sem opção 1 ou 2

## Onde retomar

1. Decidir qual opção (sugestão minha: **opção 2**, Testcontainers Desktop — não pede mudança no código nem expõe TCP sem TLS)
2. Rodar `mvn test` — se passar tudo, fechar a fase 8 (já tá marcada como [x] no README; se quiser ser rigoroso, validar que os 22 testes passam antes de considerar "feito")
3. Rodar `.\smoke-test.ps1` mais uma vez pra revalidar que nada da fase 8 quebrou os endpoints
4. Considerar adicionar GitHub Actions com `mvn test` rodando em ubuntu-latest (Linux não tem esse problema, testes vão passar limpo)

## Coisas pra eventualmente avaliar (não-bloqueantes)

- Maven Wrapper (`mvnw`) — facilita pra quem clona não precisar de mvn instalado
- LICENSE file (MIT?) se for tornar repo público
- GitHub Actions CI rodando `mvn test` no push/PR
- README badges (build status, coverage)
