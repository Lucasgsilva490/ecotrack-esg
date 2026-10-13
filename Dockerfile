# syntax=docker/dockerfile:1

# ---------- Estagio 1: build com Maven Wrapper ----------
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace

# unzip/curl sao usados pelo mvnw para baixar e extrair o Maven
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Copiar primeiro os arquivos de build melhora o cache de camadas
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

COPY src/ src/
# Os testes sao executados no pipeline (./mvnw clean verify) antes deste build; aqui so empacotamos.
RUN ./mvnw -B -DskipTests package

# ---------- Estagio 2: imagem final enxuta ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S ecotrack && adduser -S -G ecotrack ecotrack
COPY --from=build --chown=ecotrack:ecotrack /workspace/target/app.jar /app/app.jar
USER ecotrack

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# A conexao com o banco vem de SPRING_DATASOURCE_URL / _USERNAME / _PASSWORD (nao ha credenciais na imagem)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
