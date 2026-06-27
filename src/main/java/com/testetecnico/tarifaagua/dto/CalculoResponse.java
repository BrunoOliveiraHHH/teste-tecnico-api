package com.testetecnico.tarifaagua.dto;

import com.testetecnico.tarifaagua.domain.Categoria;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado detalhado do cálculo, no formato obrigatório do desafio: categoria,
 * consumoTotal, valorTotal e detalhamento por faixa.
 */
public record CalculoResponse(
        Categoria categoria,
        Integer consumoTotal,
        BigDecimal valorTotal,
        List<DetalhamentoItem> detalhamento
) {

    public record DetalhamentoItem(
            FaixaIntervalo faixa,
            Integer m3Cobrados,
            BigDecimal valorUnitario,
            BigDecimal subtotal
    ) {
    }

    public record FaixaIntervalo(
            Integer inicio,
            Integer fim
    ) {
    }
}
