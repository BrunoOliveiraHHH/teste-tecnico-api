package com.testetecnico.tarifaagua.exception;

/**
 * Recurso inexistente (ex.: tabela não encontrada). Mapeada para HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
