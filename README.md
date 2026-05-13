# financas

API pra controlar minhas finanças pessoais.

Cobre contas, categorias, transações (com idempotency em POST), orçamentos por categoria/mês, relatórios (resumo mensal, gasto por categoria, fluxo de caixa) e transações recorrentes (com scheduler).

## Stack

- Java 21
- Spring Boot 3.4
- Spring Security + JWT
- PostgreSQL 16
- Flyway (migrations)
- springdoc-openapi (Swagger UI)
- Testcontainers (testes de integração)
- Maven

## Rodando localmente

Sobe o banco:

```
docker compose up -d
```

Roda a app (precisa de Java 21 + Maven, ou abre no IntelliJ):

```
mvn spring-boot:run
```

API em `http://localhost:8080`.

### Swagger

Documentação interativa em `http://localhost:8080/swagger-ui.html`. Tem botão "Authorize" pra colar o JWT.

### Health check

`GET http://localhost:8080/actuator/health`

## Variáveis de ambiente

Tudo tem default pra dev — só precisa setar em prod.

| variável              | default                                             |
|-----------------------|-----------------------------------------------------|
| `DATABASE_URL`        | `jdbc:postgresql://localhost:5433/financas`         |
| `DATABASE_USER`       | `financas`                                          |
| `DATABASE_PASSWORD`   | `financas`                                          |
| `JWT_SECRET`          | (chave de dev — **trocar em prod**, base64 32+ bytes) |
| `JWT_EXPIRACAO_HORAS` | `8`                                                 |
| `CORS_ORIGINS`        | `http://localhost:3000,http://localhost:5173`       |
| `SERVER_PORT`         | `8080`                                              |

Pra rodar com perfil de prod:

```
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

## Testes

Os testes de integração sobem um Postgres via Testcontainers — precisa de Docker rodando.

```
mvn test
```

Smoke test fim-a-fim contra a app rodando:

```
.\smoke-test.ps1
```

## Docker

```
docker build -t financas .
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5433/financas \
  -e JWT_SECRET=<chave-base64> \
  financas
```

## Endpoints (resumo)

| método | rota                                     | o que faz                          |
|--------|------------------------------------------|------------------------------------|
| POST   | `/auth/cadastro`                         | cria usuário, devolve JWT          |
| POST   | `/auth/login`                            | autentica, devolve JWT             |
| GET    | `/me`                                    | dados do usuário logado            |
| CRUD   | `/contas`                                | contas (corrente, poupança etc)    |
| CRUD   | `/categorias`                            | categorias (RECEITA / DESPESA)     |
| CRUD   | `/transacoes`                            | RECEITA / DESPESA / TRANSFERENCIA  |
| CRUD   | `/orcamentos`                            | orçamentos por categoria/mês       |
| GET    | `/orcamentos/comparativo?ano=&mes=`      | comparativo orçado vs gasto        |
| GET    | `/relatorios/resumo-mensal?ano=&mes=`    | resumo do mês                      |
| GET    | `/relatorios/por-categoria?inicio=&fim=` | gasto agrupado por categoria       |
| GET    | `/relatorios/fluxo-caixa?meses=`         | fluxo de caixa últimos N meses     |
| CRUD   | `/transacoes-recorrentes`                | transações que se repetem mensalmente |
| POST   | `/transacoes-recorrentes/{id}/executar-agora` | dispara execução manual       |

Ver `swagger-ui.html` pra detalhes dos campos.

## Fases do projeto

- [x] Fase 0 - setup inicial
- [x] Fase 1 - usuarios + login (JWT)
- [x] Fase 2 - contas
- [x] Fase 3 - categorias
- [x] Fase 4 - transações
- [x] Fase 5 - orçamentos
- [x] Fase 6 - relatórios
- [x] Fase 7 - transações recorrentes
- [x] Fase 8 - acabamento (swagger, testes, docker)
