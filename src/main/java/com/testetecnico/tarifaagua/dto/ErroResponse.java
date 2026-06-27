package com.testetecnico.tarifaagua.dto;

import java.time.Instant;
import java.util.List;

/**
 * Corpo padronizado de resposta de erro.
 */
public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        List<String> detalhes
) {
}
