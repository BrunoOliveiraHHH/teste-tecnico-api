package com.testetecnico.tarifaagua.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record FaixaRequest(

        @NotNull(message = "O início da faixa é obrigatório")
        @PositiveOrZero(message = "O início não pode ser negativo")
        Integer inicio,

        @NotNull(message = "O fim da faixa é obrigatório")
        @Positive(message = "O fim deve ser positivo")
        Integer fim,

        @NotNull(message = "O valor unitário é obrigatório")
        @Positive(message = "O valor unitário deve ser positivo")
        BigDecimal valorUnitario
) {
}
