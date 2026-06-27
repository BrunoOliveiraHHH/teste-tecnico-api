package com.testetecnico.tarifaagua.domain;

/**
 * Categorias de consumidores suportadas. Os valores das tarifas não ficam aqui:
 * são parametrizados no banco (entidade {@link FaixaConsumo}).
 */
public enum Categoria {
    COMERCIAL,
    INDUSTRIAL,
    PARTICULAR,
    PUBLICO
}
