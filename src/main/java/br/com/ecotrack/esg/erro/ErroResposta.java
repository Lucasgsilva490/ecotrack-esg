package br.com.ecotrack.esg.erro;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResposta(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        Map<String, String> campos) {
}
