package br.com.ecotrack.esg.indicador;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndicadorRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 150, message = "nome deve ter no máximo 150 caracteres")
        String nome,

        @NotNull(message = "categoria é obrigatória (AMBIENTAL, SOCIAL ou GOVERNANCA)")
        Categoria categoria,

        @NotNull(message = "valor é obrigatório")
        @DecimalMin(value = "0.0", message = "valor não pode ser negativo")
        @Digits(integer = 15, fraction = 4, message = "valor aceita até 15 inteiros e 4 casas decimais")
        BigDecimal valor,

        @NotBlank(message = "unidade é obrigatória")
        @Size(max = 50, message = "unidade deve ter no máximo 50 caracteres")
        String unidade,

        @NotNull(message = "dataRegistro é obrigatória (formato AAAA-MM-DD)")
        LocalDate dataRegistro,

        @Size(max = 500, message = "descricao deve ter no máximo 500 caracteres")
        String descricao) {

    public Indicador toEntity() {
        return new Indicador(nome.trim(), categoria, valor, unidade.trim(), dataRegistro, descricao);
    }
}
