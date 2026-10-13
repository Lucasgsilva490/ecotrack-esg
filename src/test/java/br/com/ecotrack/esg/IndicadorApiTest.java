package br.com.ecotrack.esg;

import br.com.ecotrack.esg.indicador.Categoria;
import br.com.ecotrack.esg.indicador.Indicador;
import br.com.ecotrack.esg.indicador.IndicadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IndicadorApiTest {

    private static final String VALIDO = """
            {
              "nome": "Consumo de energia",
              "categoria": "AMBIENTAL",
              "valor": 1250.5,
              "unidade": "kWh",
              "dataRegistro": "2025-03-01",
              "descricao": "Consumo mensal da sede"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IndicadorRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void cadastraIndicadorValido() throws Exception {
        mockMvc.perform(post("/api/indicadores").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/indicadores/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Consumo de energia"))
                .andExpect(jsonPath("$.categoria").value("AMBIENTAL"))
                .andExpect(jsonPath("$.unidade").value("kWh"));
    }

    @Test
    void consultaIndicadorPorIdELista() throws Exception {
        Indicador salvo = repository.save(new Indicador("Emissões de CO2", Categoria.AMBIENTAL,
                new BigDecimal("42.5"), "tCO2e", LocalDate.of(2025, 1, 15), null));

        mockMvc.perform(get("/api/indicadores/{id}", salvo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salvo.getId()))
                .andExpect(jsonPath("$.nome").value("Emissões de CO2"))
                .andExpect(jsonPath("$.valor").value(42.5));

        mockMvc.perform(get("/api/indicadores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(salvo.getId()));
    }

    @Test
    void rejeitaEntradaInvalida() throws Exception {
        String invalido = """
                {"nome": "", "categoria": "AMBIENTAL", "valor": -1, "unidade": "kWh", "dataRegistro": "2025-03-01"}
                """;

        mockMvc.perform(post("/api/indicadores").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.valor").exists());
    }

    @Test
    void rejeitaCategoriaDesconhecida() throws Exception {
        String invalido = VALIDO.replace("AMBIENTAL", "OUTRA");

        mockMvc.perform(post("/api/indicadores").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retorna404ParaIndicadorInexistente() throws Exception {
        mockMvc.perform(get("/api/indicadores/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(delete("/api/indicadores/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluiIndicadorExistente() throws Exception {
        Indicador salvo = repository.save(new Indicador("Horas de treinamento", Categoria.SOCIAL,
                new BigDecimal("8"), "h", LocalDate.of(2025, 2, 10), "Treinamento anual"));

        mockMvc.perform(delete("/api/indicadores/{id}", salvo.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/indicadores/{id}", salvo.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthcheckRetornaUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
