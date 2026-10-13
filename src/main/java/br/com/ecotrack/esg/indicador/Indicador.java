package br.com.ecotrack.esg.indicador;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "indicadores")
public class Indicador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal valor;

    @Column(nullable = false, length = 50)
    private String unidade;

    @Column(name = "data_registro", nullable = false)
    private LocalDate dataRegistro;

    @Column(length = 500)
    private String descricao;

    protected Indicador() {
        // exigido pelo JPA
    }

    public Indicador(String nome, Categoria categoria, BigDecimal valor, String unidade,
                     LocalDate dataRegistro, String descricao) {
        this.nome = nome;
        this.categoria = categoria;
        this.valor = valor;
        this.unidade = unidade;
        this.dataRegistro = dataRegistro;
        this.descricao = descricao;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Categoria getCategoria() { return categoria; }
    public BigDecimal getValor() { return valor; }
    public String getUnidade() { return unidade; }
    public LocalDate getDataRegistro() { return dataRegistro; }
    public String getDescricao() { return descricao; }
}
