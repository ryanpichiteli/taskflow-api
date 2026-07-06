# TaskFlow API

API REST para gerenciamento de projetos e tarefas em equipe, construída com Spring Boot 4 e Java 17. Projeto de portfólio focado em demonstrar boas práticas de arquitetura, segurança e testes em uma API Java moderna.

## Visão geral do domínio

Usuários criam **projetos**, convidam **membros por e-mail**, e organizam o trabalho em **tarefas** com status, prioridade e responsável. Cada tarefa pode receber **comentários** dos membros do projeto.

Regras de negócio aplicadas na camada de serviço:

- Apenas membros de um projeto conseguem ver/criar tarefas e comentários nele.
- Apenas o *owner* do projeto (ou um ADMIN) pode atualizar/excluir o projeto e enviar convites.
- **Entrar num projeto exige convite aceito** — não existe endpoint que adicione alguém como membro à força a partir de um id. O owner convida por e-mail (`POST /api/projects/{id}/invitations`); o convite só vira participação de fato quando o próprio convidado aceita (`POST /api/invitations/{id}/accept`). Convites só podem ser enviados para e-mails já cadastrados, não podem duplicar um convite pendente, nem ser enviados para quem já é membro.
- Uma tarefa só pode ser excluída pelo seu criador, pelo owner do projeto, ou por um ADMIN.
- O responsável (*assignee*) de uma tarefa precisa ser membro do projeto.
- Um comentário só pode ser excluído pelo seu autor ou por um ADMIN.

## Stack técnica

| Camada | Tecnologia |
|---|---|
| Linguagem / Runtime | Java 17, Spring Boot 4.1 |
| Web | Spring Web MVC |
| Persistência | Spring Data JPA (Hibernate) + PostgreSQL |
| Migrations | Flyway |
| Segurança | Spring Security + JWT (JJWT) |
| Mapeamento DTO ↔ Entidade | MapStruct |
| Documentação | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, AssertJ, Testcontainers |
| Observabilidade | Spring Boot Actuator |
| Empacotamento | Docker multi-stage build + Docker Compose |

## Arquitetura

```
controller/   → endpoints REST, validação de entrada, Swagger annotations
service/      → regras de negócio e autorização
repository/   → Spring Data JPA + Specifications para filtros dinâmicos
entity/       → modelo de domínio JPA
dto/          → contratos de entrada/saída (records)
mapper/       → conversão entidade ↔ DTO (MapStruct)
security/     → JWT (geração/validação), UserDetails, filtro de autenticação
exception/    → exceções de domínio + handler global (@RestControllerAdvice)
config/       → Spring Security, OpenAPI
```

Autorização é feita explicitamente na camada de serviço (não em SpEL espalhado pelos controllers), o que deixa as regras de negócio fáceis de testar isoladamente com Mockito — veja `ProjectServiceTest` e `TaskServiceTest`.

## Como rodar

### Opção 1 — Docker Compose (recomendado)

```bash
docker compose up --build
```

Sobe Postgres + a API. A API roda em `http://localhost:8080` e aplica as migrations do Flyway automaticamente na inicialização.

### Opção 2 — localmente com Maven

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

### Opção 3 — Railway (deploy real)

O repositório já inclui `railway.json` configurado para buildar a partir do `Dockerfile` (builder `DOCKERFILE`). Passos no painel do Railway:

1. Crie o serviço a partir deste repositório (Railway detecta o `railway.json` e builda a imagem automaticamente — não precisa de `docker compose`, que só existe para uso local).
2. Adicione um plugin **PostgreSQL** ao projeto.
3. Nas variáveis de ambiente do serviço da API, aponte para o Postgres via *reference variables*:

   ```
   DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
   DB_USERNAME=${{Postgres.PGUSER}}
   DB_PASSWORD=${{Postgres.PGPASSWORD}}
   JWT_SECRET=<uma chave base64 própria, diferente da usada em dev>
   ```

4. O Railway injeta automaticamente a variável `PORT`, que a aplicação já respeita (`server.port` em `application.yml`).

Não é necessário (nem funciona) rodar `docker compose up --build` como *start command* — o Railway já builda a imagem do Dockerfile e executa o `ENTRYPOINT` dele (`java -jar app.jar`) diretamente; `docker compose` não existe dentro do container em produção.

### Documentação interativa

Com a aplicação no ar: `http://localhost:8080/swagger-ui.html`

## Testando a API

Fluxo básico via curl:

```bash
# Registrar usuário e obter token JWT
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Ada Lovelace","email":"ada@taskflow.dev","password":"S3cret!23"}'

# Criar projeto (substitua $TOKEN pelo token retornado acima)
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"TaskFlow Portfolio","description":"Projeto de demonstracao"}'

# Convidar um membro por e-mail (substitua $PROJECT_ID; o e-mail precisa ter conta cadastrada)
curl -X POST http://localhost:8080/api/projects/$PROJECT_ID/invitations \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"email":"grace@taskflow.dev"}'

# A pessoa convidada aceita (autenticada com o próprio token, substitua $INVITATION_ID)
curl -X POST http://localhost:8080/api/invitations/$INVITATION_ID/accept \
  -H "Authorization: Bearer $GRACE_TOKEN"

# Criar tarefa no projeto (substitua $PROJECT_ID)
curl -X POST http://localhost:8080/api/projects/$PROJECT_ID/tasks \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Configurar CI/CD","priority":"HIGH"}'
```

## Testes automatizados

```bash
./mvnw test
```

Roda a suíte de testes unitários (services, com Mockito) que cobre as regras de autorização e os fluxos de autenticação — sem depender de infraestrutura externa.

Há também testes de integração (`@Tag("integration")`) que sobem um PostgreSQL real via Testcontainers e exercitam a API de ponta a ponta via MockMvc (registro → criação de projeto → tarefa → comentário, incluindo os casos de acesso negado). Eles ficam fora do `mvn test` padrão por dependerem de Docker; para rodá-los:

```bash
./mvnw test -Dgroups=integration
```

## Principais endpoints

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Cria usuário e retorna JWT | Pública |
| POST | `/api/auth/login` | Autentica e retorna JWT | Pública |
| GET | `/api/users/me` | Dados do usuário autenticado | JWT |
| GET | `/api/users/{id}` | Busca usuário por id | JWT |
| GET | `/api/users` | Lista usuários (paginado) | JWT + ADMIN |
| POST | `/api/projects` | Cria projeto | JWT |
| GET | `/api/projects` | Lista projetos do usuário (filtros: `name`, `status`; paginado) | JWT |
| GET \| PUT \| DELETE | `/api/projects/{id}` | Busca / atualiza / remove projeto | JWT (membro / owner+ADMIN) |
| DELETE | `/api/projects/{id}/members/{userId}` | Remove membro | JWT (owner + ADMIN) |
| POST | `/api/projects/{id}/invitations` | Convida um usuário pelo e-mail | JWT (owner + ADMIN) |
| GET | `/api/projects/{id}/invitations` | Lista convites enviados do projeto (paginado) | JWT (owner + ADMIN) |
| GET | `/api/invitations/me` | Lista convites recebidos pelo usuário autenticado (filtro `status`; paginado) | JWT |
| POST | `/api/invitations/{id}/accept` | Aceita um convite — só então vira membro | JWT (o próprio convidado) |
| POST | `/api/invitations/{id}/decline` | Recusa um convite | JWT (o próprio convidado) |
| POST | `/api/projects/{projectId}/tasks` | Cria tarefa | JWT (membro) |
| GET | `/api/projects/{projectId}/tasks` | Lista tarefas (filtros: `status`, `priority`, `assigneeId`, `title`; paginado) | JWT (membro) |
| GET \| PUT \| DELETE | `/api/tasks/{id}` | Busca / atualiza / remove tarefa | JWT (membro / criador+owner+ADMIN) |
| PATCH | `/api/tasks/{id}/status` | Atualiza só o status da tarefa | JWT (membro) |
| POST \| GET | `/api/tasks/{taskId}/comments` | Cria / lista comentários | JWT (membro) |
| DELETE | `/api/comments/{id}` | Remove comentário | JWT (autor + ADMIN) |

Lista completa e interativa no Swagger UI (`/swagger-ui.html`), incluindo os schemas de request/response gerados a partir dos DTOs.

## Formato das respostas

Todas as respostas são JSON. Não há envelope genérico (`{ data: ... }`) — o corpo é o próprio recurso ou uma lista paginada.

### Autenticação — `POST /api/auth/register` e `/api/auth/login`

`201 Created` / `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": "6ec10893-51a2-4e52-9858-6fa9311f128e",
    "name": "Ada Lovelace",
    "email": "ada@taskflow.dev",
    "role": "USER",
    "createdAt": "2026-07-06T13:28:51.160400Z"
  }
}
```

Use o `token` no header `Authorization: Bearer <token>` em todas as chamadas autenticadas.

### Recurso único — ex. `GET /api/projects/{id}`

```json
{
  "id": "37225a58-8f86-4920-a22f-6e359e73743d",
  "name": "Outro Projeto",
  "description": "teste filtro",
  "status": "ACTIVE",
  "owner": { "id": "6ec1...", "name": "Ada Lovelace", "email": "ada@taskflow.dev", "role": "USER", "createdAt": "..." },
  "members": [ { "id": "6ec1...", "name": "Ada Lovelace", "...": "..." } ],
  "createdAt": "2026-07-06T13:28:51.555001Z",
  "updatedAt": "2026-07-06T13:28:51.555001Z"
}
```

Associações (`owner`, `members`, `assignee`, `createdBy`, `author`) sempre vêm expandidas como objeto — nunca só o id — para evitar uma segunda chamada em telas de listagem.

### Convite — ex. `POST /api/projects/{id}/invitations`

```json
{
  "id": "9f3e2b1a-0c4d-4e5f-8a6b-7c8d9e0f1a2b",
  "projectId": "37225a58-8f86-4920-a22f-6e359e73743d",
  "projectName": "Outro Projeto",
  "invitedUser": { "id": "...", "name": "Grace Hopper", "email": "grace@taskflow.dev", "role": "USER", "createdAt": "..." },
  "invitedBy": { "id": "...", "name": "Ada Lovelace", "email": "ada@taskflow.dev", "role": "USER", "createdAt": "..." },
  "status": "PENDING",
  "createdAt": "2026-07-06T16:20:00Z",
  "respondedAt": null
}
```

`status` muda para `ACCEPTED` ou `DECLINED` (com `respondedAt` preenchido) após a chamada em `/api/invitations/{id}/accept` ou `/decline` — só nesse momento o convidado passa a aparecer em `members` do projeto.

### Lista paginada — ex. `GET /api/projects` ou `/api/projects/{id}/tasks`

Toda listagem usa o mesmo envelope (`PageResponse`), independente do recurso:

```json
{
  "content": [ { "id": "...", "name": "...", "...": "..." } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

Parâmetros de paginação padrão do Spring (`page`, `size`, `sort=campo,asc|desc`) funcionam em qualquer rota de listagem, além dos filtros específicos de cada recurso (`name`, `status`, `priority`, etc.).

### Erros — formato único para toda a API

Qualquer erro (validação, não encontrado, acesso negado, etc.) retorna o mesmo formato via `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-07-06T13:27:00.315465Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You are not a member of this project",
  "path": "/api/projects/37225a58-8f86-4920-a22f-6e359e73743d",
  "fieldErrors": []
}
```

Em erros de validação (`400`), `fieldErrors` vem preenchido com um item por campo inválido:

```json
{
  "timestamp": "2026-07-06T13:27:00.315465Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "fieldErrors": [
    { "field": "password", "message": "Password must be at least 8 characters long" }
  ]
}
```

| Status | Quando ocorre |
|---|---|
| `200 / 201 / 204` | Sucesso (leitura / criação / exclusão sem corpo) |
| `400` | Payload inválido (`@Valid` falhou) — corpo com `fieldErrors` |
| `401` | Credenciais inválidas no login |
| `403` | Token ausente/inválido, ou usuário autenticado sem permissão (ex.: não é membro do projeto, não é owner) |
| `404` | Recurso inexistente (projeto, tarefa, comentário, usuário) — inclui convidar um e-mail sem conta cadastrada |
| `409` | E-mail já cadastrado no registro; convite duplicado/já respondido; convidar quem já é membro |
| `500` | Erro inesperado (logado no servidor com stack trace; nunca vaza detalhe interno na resposta) |

## Decisões de design

- **Specifications (Criteria API)** para filtros dinâmicos de projetos/tarefas em vez de múltiplos métodos de repositório — evita explosão combinatória de queries.
- **Autorização na camada de serviço**, não em `@PreAuthorize` com SpEL complexo — mantém as regras legíveis e testáveis com um mock simples de repositório.
- **MapStruct** para mapeamento DTO/entidade — evita boilerplate manual e erros de mapeamento silenciosos.
- **Sem refresh token**: o token JWT tem validade configurável (`taskflow.security.jwt.expiration-minutes`, padrão 24h) — suficiente para o escopo deste projeto; um mecanismo de refresh/revogação seria o próximo passo natural em um cenário de produção.
- **Convite por e-mail em vez de "adicionar membro por id"**: entrar num projeto exige consentimento explícito do convidado (`accept`/`decline`), não apenas o owner informar um id qualquer — evita adicionar alguém sem que ela saiba, e não expõe UUIDs internos no fluxo do usuário. Um índice único parcial no banco (`project_invitations`, `WHERE status = 'PENDING'`) garante que não existam dois convites pendentes duplicados para o mesmo par projeto/usuário, mesmo sob concorrência.
