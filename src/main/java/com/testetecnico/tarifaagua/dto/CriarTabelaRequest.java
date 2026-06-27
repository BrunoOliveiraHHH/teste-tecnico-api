package com.testetecnico.tarifaagua.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload de criação de uma tabela tarifária completa (categorias + faixas).
 */
public record CriarTabelaRequest(

        @NotBlank(message = "O nome da tabela é obrigatório")
        String nome,

        LocalDate dataVigencia,

        @NotEmpty(message = "Informe ao menos uma categoria")
        @Valid
        List<CategoriaFaixasRequest> categorias
) {
}
