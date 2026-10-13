package br.com.ecotrack.esg.indicador;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndicadorResponse(
        Long id,
        String nome,
        Categoria categoria,
        BigDecimal valor,
        String unidade,
        LocalDate dataRegistro,
        String descricao) {

    public static IndicadorResponse from(Indicador i) {
        return new IndicadorResponse(i.getId(), i.getNome(), i.getCategoria(), i.getValor(),
                i.getUnidade(), i.getDataRegistro(), i.getDescricao());
    }
}
