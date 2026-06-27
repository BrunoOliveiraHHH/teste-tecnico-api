package com.testetecnico.tarifaagua.controller;

import com.testetecnico.tarifaagua.dto.CalculoRequest;
import com.testetecnico.tarifaagua.dto.CalculoResponse;
import com.testetecnico.tarifaagua.service.CalculoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calculos")
public class CalculoController {

    private final CalculoService service;

    public CalculoController(CalculoService service) {
        this.service = service;
    }

    /** Calcula o valor a pagar de forma progressiva por faixas. */
    @PostMapping
    public CalculoResponse calcular(@Valid @RequestBody CalculoRequest request) {
        return service.calcular(request);
    }
}
