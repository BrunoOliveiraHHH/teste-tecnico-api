package com.testetecnico.tarifaagua.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tabela tarifária retornada pela API, com suas categorias e faixas.
 */
public record TabelaResponse(
        Long id,
        String nome,
        LocalDate dataVigencia,
        boolean ativo,
        List<CategoriaFaixasResponse> categorias
) {

    public record CategoriaFaixasResponse(
            String categoria,
            List<FaixaResponse> faixas
    ) {
    }

    public record FaixaResponse(
            Long id,
            Integer inicio,
            Integer fim,
            BigDecimal valorUnitario
    ) {
    }
}
