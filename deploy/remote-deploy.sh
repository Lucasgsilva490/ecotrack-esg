#!/usr/bin/env bash
# Executado NO SERVIDOR pelo pipeline: baixa a imagem, sobe os containers e valida a saude.
# Uso: bash remote-deploy.sh <staging|production> <porta>
# Requisitos no servidor: Docker, Docker Compose v2 e curl.
set -euo pipefail

ENV_NAME="${1:?informe o ambiente (staging|production)}"
APP_PORT="${2:?informe a porta publicada}"
PROJECT="ecotrack-${ENV_NAME}"
COMPOSE=(docker compose -p "$PROJECT" --env-file .env -f docker-compose.yml)

"${COMPOSE[@]}" pull
"${COMPOSE[@]}" up -d --remove-orphans

echo "Aguardando /actuator/health em http://localhost:${APP_PORT} ..."
for i in $(seq 1 30); do
  if curl -fsS "http://localhost:${APP_PORT}/actuator/health" | grep -q '"status":"UP"'; then
    echo "OK: ${PROJECT} saudavel (tentativa ${i})."
    exit 0
  fi
  sleep 5
done

echo "ERRO: ${PROJECT} nao ficou saudavel em 150 s. Ultimos logs:" >&2
"${COMPOSE[@]}" ps >&2 || true
"${COMPOSE[@]}" logs --tail=100 app >&2 || true
exit 1
