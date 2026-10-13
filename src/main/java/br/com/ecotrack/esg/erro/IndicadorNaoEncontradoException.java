package br.com.ecotrack.esg.erro;

public class IndicadorNaoEncontradoException extends RuntimeException {

    public IndicadorNaoEncontradoException(Long id) {
        super("Indicador não encontrado: " + id);
    }
}
