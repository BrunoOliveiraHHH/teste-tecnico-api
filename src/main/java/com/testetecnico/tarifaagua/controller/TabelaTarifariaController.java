package com.testetecnico.tarifaagua.controller;

import com.testetecnico.tarifaagua.dto.CriarTabelaRequest;
import com.testetecnico.tarifaagua.dto.TabelaResponse;
import com.testetecnico.tarifaagua.service.TabelaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tabelas-tarifarias")
public class TabelaTarifariaController {

    private final TabelaService service;

    public TabelaTarifariaController(TabelaService service) {
        this.service = service;
    }

    /** Cria uma tabela tarifária completa (categorias + faixas + valores). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TabelaResponse criar(@Valid @RequestBody CriarTabelaRequest request) {
        return service.criar(request);
    }

    /** Lista todas as tabelas tarifárias ativas. */
    @GetMapping
    public List<TabelaResponse> listar() {
        return service.listar();
    }

    /** Exclui (logicamente) uma tabela tarifária. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
