# EcoTrack ESG — Documentação Técnica

**Integrantes:** _(preencher nomes e RA)_
**Disciplina / Professor:** _(preencher)_
**Repositório:** _(preencher URL real)_

## 1. Objetivo

API REST para registrar e consultar indicadores ESG, usada como base para demonstrar o ciclo DevOps: CI/CD, testes automatizados, containerização, Docker Compose e deploy em staging e produção.

## 2. Arquitetura

```
Cliente → app (Spring Boot, 8080) → db (PostgreSQL 16)
          /actuator/health          volume pgdata + rede ecotrack-net
```

Camadas: Controller → Repository → Entity (sem service, por simplicidade). Erros tratados em `GlobalExceptionHandler` (400/404).

## 3. Tecnologias

Java 21, Spring Boot 3.3.5, Maven Wrapper, PostgreSQL 16.4, H2 (testes), JUnit 5/MockMvc, Docker, Docker Compose, GitHub Actions, GHCR.

## 4. Etapas do pipeline

1. **build-test:** checkout → Java 21 (cache Maven) → `./mvnw -B clean verify` → artefato `surefire-reports`.
2. **docker:** build da imagem; push ao GHCR (tags: SHA e `latest`) somente em push/manual na `main`.
3. **deploy-staging:** Environment `staging`, SSH, `docker compose pull/up`, verificação de `/actuator/health`.
4. **deploy-production:** Environment `production` (aprovação manual recomendada), mesma imagem do staging, configuração própria.

Pull requests: apenas build, testes e build da imagem.

## 5. Estratégia Docker

Multi-stage (JDK para build, JRE Alpine para execução), usuário não root, sem credenciais na imagem, configuração por variáveis de ambiente, healthcheck no Compose via `/actuator/health`. Compose local com `depends_on: service_healthy`, volume nomeado e rede dedicada.

## 6. Comandos principais

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose ps
docker compose logs -f app
Invoke-RestMethod http://localhost:8080/actuator/health
.\mvnw.cmd clean verify
docker compose down          # mantém dados
docker compose down -v       # APAGA os dados do banco
```

## 7. Staging e produção

Deploy por SSH em servidor Linux com Docker/Compose, com GitHub Environments `staging` e `production`, secrets independentes (`SSH_HOST`, `SSH_USER`, `SSH_PRIVATE_KEY`, `POSTGRES_PASSWORD`, opcionais `SSH_PORT`, `SSH_KNOWN_HOSTS`). Staging: porta 8081, banco `ecotrack_staging`. Produção: porta 8080, banco `ecotrack_production`. Detalhes no README (seção 15).

**Status real dos ambientes:** _(preencher após executar: URLs/IPs reais, data do deploy, SHA implantado)_

## 8. Pontos de atenção técnicos

| Ponto de atenção | Como o projeto trata |
|---|---|
| App iniciar antes do banco | `depends_on: service_healthy` + `restart: unless-stopped` |
| Perda de dados ao recriar containers | volume nomeado `pgdata` |
| Credenciais no repositório | `.env` no `.gitignore`; secrets por Environment |
| Esquema do banco | `ddl-auto=update` (MVP); ideal: Flyway/Liquibase |
| Permissão do `mvnw` (Linux/CI) | `chmod +x` no pipeline e no Dockerfile; `.gitattributes` força LF |
| Publicar no GHCR | permissões do `GITHUB_TOKEN` e visibilidade do pacote |
| Deploy sem infraestrutura | job avisa "NÃO realizado" em vez de fingir sucesso |

**Desafios reais encontrados pelo grupo:** _(registrar aqui, com data e solução)_

## 9. Evidências a capturar (prints reais)

- [ ] Pipeline verde na aba Actions (jobs build-test, docker, deploy-staging, deploy-production).
- [ ] Relatório/log dos testes (`Tests run: ..., Failures: 0`).
- [ ] Imagem publicada em *Packages* (GHCR) com a tag do SHA.
- [ ] `docker compose ps` mostrando `app` e `db` como `healthy`.
- [ ] Chamadas reais à API (POST 201, GET, 404) e `/actuator/health`.
- [ ] Environments `staging` e `production` configurados (sem expor secrets) e regra de aprovação.
- [ ] Staging respondendo `/actuator/health` no servidor real.
- [ ] Produção respondendo `/actuator/health` no servidor real.
- [ ] *Summary* do run indicando deploys realizados.

## 10. Checklist de entrega

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
