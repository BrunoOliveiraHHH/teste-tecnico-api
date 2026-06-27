package com.testetecnico.tarifaagua.dto;

import com.testetecnico.tarifaagua.domain.Categoria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CategoriaFaixasRequest(

        @NotNull(message = "A categoria é obrigatória")
        Categoria categoria,

        @NotEmpty(message = "Informe ao menos uma faixa para a categoria")
        @Valid
        List<FaixaRequest> faixas
) {
}
