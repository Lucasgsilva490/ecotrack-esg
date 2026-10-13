package br.com.ecotrack.esg.indicador;

import br.com.ecotrack.esg.erro.IndicadorNaoEncontradoException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller direto sobre o repository: a API so tem operacoes CRUD simples,
 * entao uma camada de service nao acrescentaria nada neste MVP.
 */
@RestController
@RequestMapping("/api/indicadores")
public class IndicadorController {

    private final IndicadorRepository repository;

    public IndicadorController(IndicadorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<IndicadorResponse> listar() {
        return repository.findAll(Sort.by("id")).stream().map(IndicadorResponse::from).toList();
    }

    @GetMapping("/{id}")
    public IndicadorResponse buscar(@PathVariable Long id) {
        return repository.findById(id)
                .map(IndicadorResponse::from)
                .orElseThrow(() -> new IndicadorNaoEncontradoException(id));
    }

    @PostMapping
    public ResponseEntity<IndicadorResponse> criar(@Valid @RequestBody IndicadorRequest request) {
        Indicador salvo = repository.save(request.toEntity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(salvo.getId()).toUri();
        return ResponseEntity.created(location).body(IndicadorResponse.from(salvo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new IndicadorNaoEncontradoException(id);
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
