package com.testetecnico.tarifaagua.exception;

/**
 * Violação de regra de negócio (faixas inconsistentes, consumo sem cobertura
 * etc.). Mapeada para HTTP 422.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
