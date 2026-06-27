package com.testetecnico.tarifaagua.dto;

import com.testetecnico.tarifaagua.domain.Categoria;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Entrada do cálculo: categoria + consumo total (m³).
 */
public record CalculoRequest(

        @NotNull(message = "A categoria é obrigatória")
        Categoria categoria,

        @NotNull(message = "O consumo é obrigatório")
        @PositiveOrZero(message = "O consumo não pode ser negativo")
        Integer consumo
) {
}
