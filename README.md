# EcoTrack ESG

API REST simples para registrar e consultar indicadores ESG (Ambiental, Social e Governança), criada como projeto acadêmico para demonstrar práticas de **DevOps**: CI/CD, testes automatizados, containerização, Docker Compose e deploy em staging e produção.

> A aplicação de negócio é propositalmente pequena. O foco é o ciclo de vida DevOps.

## 1. Objetivo

Demonstrar, com uma aplicação real: build automatizado, testes automatizados, imagem Docker, orquestração com Docker Compose (variáveis, volumes e redes), publicação de imagem no GHCR e deploy automatizado em dois ambientes (staging e produção).

## 2. Tecnologias

Java 21 · Spring Boot 3.3.5 (Web, Data JPA, Validation, Actuator) · Maven + Maven Wrapper · PostgreSQL 16 · H2 (somente testes) · JUnit 5 + MockMvc · Docker · Docker Compose · GitHub Actions · GitHub Container Registry (GHCR).

## 3. Arquitetura resumida

```
Cliente ──HTTP──> [app: Spring Boot :8080] ──JDBC──> [db: PostgreSQL :5432]
                          │                                  │
                  /actuator/health                   volume nomeado (pgdata)
                  (inclui teste do banco)            rede dedicada (ecotrack-net)
```

`Controller → Repository → Entity`. Não há camada de service porque as operações são CRUD simples (decisão deliberada de simplicidade).

## 4. Pré-requisitos

- Para rodar com Docker: Docker Desktop (ou Docker Engine + Compose v2).
- Para rodar os testes localmente: JDK 21 (o Maven é baixado pelo `mvnw`).
- Para o pipeline: conta e repositório no GitHub.

## 5. Estrutura de pastas

```
ecotrack-esg/
├── .github/
│   ├── workflows/ci-cd.yml        # pipeline completo
│   └── actions/deploy/action.yml  # passos de deploy via SSH (reutilizado por staging e produção)
├── deploy/
│   ├── docker-compose.yml         # compose de deploy (usa imagem do GHCR)
│   ├── remote-deploy.sh           # executado no servidor: pull, up e verificação de saúde
│   ├── staging/.env.example       # referência: porta 8081, banco ecotrack_staging
│   └── production/.env.example    # referência: porta 8080, banco ecotrack_production
├── src/main/java/br/com/ecotrack/esg/   # código (indicador/, erro/)
├── src/main/resources/application.yml
├── src/test/                      # testes + application-test.yml (H2)
├── .mvn/wrapper/ · mvnw · mvnw.cmd
├── Dockerfile · .dockerignore
├── docker-compose.yml             # ambiente local
├── .env.example · .gitignore · .gitattributes
├── pom.xml
├── README.md
└── DOCUMENTACAO_TECNICA.md
```

## 6. Executar localmente com Docker Compose

PowerShell:

```powershell
Copy-Item .env.example .env
notepad .env          # troque POSTGRES_PASSWORD
docker compose up --build -d
```

Linux/macOS:

```bash
cp .env.example .env
nano .env             # troque POSTGRES_PASSWORD
docker compose up --build -d
```

Variáveis do `.env`: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` (obrigatória) e `APP_PORT` (padrão 8080). O primeiro build baixa o Maven e as dependências e pode levar alguns minutos.

## 7. Executar os testes

```powershell
.\mvnw.cmd clean verify
```
```bash
./mvnw clean verify
```

Os testes usam H2 em memória (perfil `test`); **não precisam de PostgreSQL, Docker nem internet**, exceto o download inicial das dependências Maven. Relatórios em `target/surefire-reports/`.

Cobertura dos testes: inicialização do contexto, cadastro válido (201 + `Location`), consulta por id e listagem, validação inválida (400), categoria desconhecida (400), 404 em consulta/exclusão inexistente, exclusão (204) e `/actuator/health` = UP.

## 8. Endpoints

| Método | Rota | Resposta |
|---|---|---|
| GET | `/api/indicadores` | 200 + lista |
| GET | `/api/indicadores/{id}` | 200 ou 404 |
| POST | `/api/indicadores` | 201 ou 400 |
| DELETE | `/api/indicadores/{id}` | 204 ou 404 |
| GET | `/actuator/health` | 200 `{"status":"UP"}` |

Campos: `nome` (obrigatório), `categoria` (`AMBIENTAL`, `SOCIAL`, `GOVERNANCA`), `valor` (≥ 0), `unidade` (obrigatória), `dataRegistro` (`AAAA-MM-DD`), `descricao` (opcional).

PowerShell:

```powershell
$body = '{"nome":"Consumo de energia","categoria":"AMBIENTAL","valor":1250.5,"unidade":"kWh","dataRegistro":"2025-03-01","descricao":"Consumo mensal"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/indicadores -ContentType "application/json" -Body $body
Invoke-RestMethod http://localhost:8080/api/indicadores
Invoke-RestMethod http://localhost:8080/api/indicadores/1
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod -Method Delete http://localhost:8080/api/indicadores/1
```

Linux/macOS:

```bash
curl -i -X POST http://localhost:8080/api/indicadores -H "Content-Type: application/json" \
  -d '{"nome":"Consumo de energia","categoria":"AMBIENTAL","valor":1250.5,"unidade":"kWh","dataRegistro":"2025-03-01"}'
curl http://localhost:8080/api/indicadores
curl http://localhost:8080/actuator/health
```

## 9. Logs, status e healthchecks

```powershell
docker compose ps                  # STATUS deve mostrar (healthy) em app e db
docker compose logs -f app         # logs da aplicação
docker compose logs db
docker inspect --format "{{.State.Health.Status}}" ecotrack-esg-app-1
```

## 10. Parar e remover

```powershell
docker compose stop                # para, mantendo containers e dados
docker compose down                # remove containers e rede; MANTÉM os dados (volume)
docker compose down -v             # ⚠️ remove também o volume: TODOS OS DADOS DO BANCO SERÃO PERDIDOS
```

## 11. Dockerfile

Build multi-stage. Estágio 1 (`eclipse-temurin:21-jdk-noble`): instala `unzip`/`curl` (usados pelo `mvnw`), copia `.mvn`, `mvnw` e `pom.xml` antes do código (melhor cache) e gera `target/app.jar` com `./mvnw -DskipTests package` (os testes rodam antes, no pipeline). Estágio 2 (`eclipse-temurin:21-jre-alpine`): imagem enxuta, usuário **não root** (`ecotrack`), porta 8080, sem `.env` nem credenciais (`.dockerignore` os exclui). As tags fixam a versão major do Java (21); não usa `latest`. Requer apenas Docker Desktop padrão (a linha `# syntax=` usa o frontend padrão do BuildKit).

## 12. docker-compose.yml

- `db`: `postgres:16.4-alpine`, credenciais via variáveis, volume nomeado `pgdata`, `healthcheck` com `pg_isready`.
- `app`: build local, conecta em `jdbc:postgresql://db:5432/...` (hostname = nome do serviço, não `localhost`), `depends_on: condition: service_healthy` (espera o banco), `healthcheck` em `/actuator/health`, `restart: unless-stopped`, porta externa `APP_PORT`.
- Rede dedicada `ecotrack-net`.
- A aplicação lê `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`; o `application.yml` **não tem valores padrão** para elas (nenhuma credencial embutida).
- **`ddl-auto=update`**: cria/ajusta as tabelas sozinho, o que dispensa migrações num MVP acadêmico. Não use `create`/`create-drop` em produção (apagam dados). Em um sistema real, o recomendado é Flyway/Liquibase.

## 13. Pipeline CI/CD (`.github/workflows/ci-cd.yml`)

Gatilhos: push na `main`, pull request para a `main` e `workflow_dispatch`.

| Job | O que faz | Quando |
|---|---|---|
| `build-test` | checkout, Java 21 + cache Maven, `./mvnw -B clean verify`, publica `surefire-reports` | sempre; sem secrets |
| `docker` | builda a imagem; faz login e push no GHCR | build sempre; **push só em push/manual na `main`** |
| `deploy-staging` | Environment `staging`; deploy automático via SSH + verificação de saúde | após o push da imagem |
| `deploy-production` | Environment `production`; só implanta se o staging foi realmente implantado | após `deploy-staging` |

Pull requests rodam build, testes e build da imagem, **sem publicar e sem deploy**. Actions fixadas em versões explícitas; permissões mínimas (`contents: read`; `packages: write` apenas no job que publica; `packages: read` nos deploys).

## 14. Publicação da imagem

Imagem: `ghcr.io/<dono>/<repositório>` (em minúsculas). Tags: o **SHA completo do commit** (usado nos deploys, rastreável) e `latest` (apenas conveniência). Usa `GITHUB_TOKEN`; não há token pessoal. Pode ser necessário: em *Settings → Actions → General → Workflow permissions*, permitir leitura e escrita; e, depois do primeiro push, ajustar a visibilidade/vínculo do pacote em *Packages*. **A publicação só pode ser validada no GitHub** (não foi executada aqui).

## 15. Configurar staging e produção

O deploy é **real, por SSH**: o pipeline copia `deploy/docker-compose.yml` e `remote-deploy.sh` ao servidor, grava um `.env` por ambiente, faz `docker compose pull/up` e confirma `/actuator/health`. A mesma imagem (mesmo SHA) vai aos dois ambientes; mudam apenas o projeto Compose (`ecotrack-staging`/`ecotrack-production`), a porta, o banco e as credenciais.

**Requisitos mínimos do servidor:** Linux com acesso SSH, Docker Engine, Docker Compose v2 e `curl`; usuário SSH no grupo `docker`. Pode ser **um único servidor** hospedando os dois ambientes (portas 8081 e 8080, volumes e redes isolados pelo nome do projeto) ou dois servidores. Uma VM gratuita/barata de qualquer provedor (ou uma VM local acessível) serve; nada neste repositório presume um host específico.

**Passos:**

1. No GitHub: *Settings → Environments → New environment* → crie `staging` e `production`.
2. Em cada Environment, cadastre os **secrets**:

| Secret | Obrigatório | Descrição |
|---|---|---|
| `SSH_HOST` | sim | endereço do servidor |
| `SSH_USER` | sim | usuário SSH |
| `SSH_PRIVATE_KEY` | sim | chave privada (par dedicado ao deploy) |
| `POSTGRES_PASSWORD` | sim | senha do banco **diferente em cada ambiente** (sem aspa simples `'`) |
| `SSH_PORT` | não | padrão 22 |
| `SSH_KNOWN_HOSTS` | recomendado | saída de `ssh-keyscan -p 22 SEU_HOST`; sem ele, o pipeline confia na primeira chave recebida |

3. Opcionalmente, *Variables* por Environment: `APP_PORT` (padrões: 8081 staging, 8080 produção), `POSTGRES_DB`, `POSTGRES_USER`.
4. Em `production`, ative **Required reviewers** (aprovação manual antes do deploy de produção).
5. Faça push na `main`.

**Sem os secrets configurados:** build, testes e publicação da imagem continuam funcionando. O job de deploy **não finge sucesso**: emite um `warning` e escreve no *Summary* do run "Deploy NÃO realizado" com os secrets que faltam; a produção também é pulada se o staging não foi implantado.

Deploy manual (referência), no servidor: `docker compose -p ecotrack-staging --env-file .env -f docker-compose.yml up -d` (veja `deploy/*/.env.example`).

## 16. Prints reais a capturar

Capture **somente após executar de verdade**. Veja a lista em `DOCUMENTACAO_TECNICA.md`.

## 17. Limitações e pendências

- Build/testes Maven, build Docker, Compose, publicação no GHCR e deploys **não foram executados no ambiente em que este projeto foi gerado** (sem Docker e sem acesso ao Maven Central). Execute e confira localmente e no GitHub.
- Os deploys reais dependem de servidor SSH e dos secrets acima.
- Sem autenticação, paginação, atualização (PUT/PATCH) ou migrações de banco (escopo propositalmente reduzido).
- `ssh-keyscan` sem `SSH_KNOWN_HOSTS` é confiança no primeiro contato (aceitável para fins acadêmicos).

## 18. Checklist de entrega

Marque somente com comprovação.

- [ ] Projeto compactado em ZIP com estrutura organizada.
- [ ] Dockerfile funcional.
- [ ] docker-compose.yml funcional.
- [ ] Pipeline com build automático.
- [ ] Testes automatizados executados.
- [ ] Deploy automatizado em staging.
- [ ] Deploy automatizado em produção.
- [ ] README com instruções e evidências.
- [ ] Documentação técnica em PDF ou PPT.
- [ ] Prints reais do pipeline.
- [ ] Prints reais de staging e produção.
