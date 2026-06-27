package com.testetecnico.tarifaagua.repository;

import com.testetecnico.tarifaagua.domain.Categoria;
import com.testetecnico.tarifaagua.domain.FaixaConsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FaixaConsumoRepository extends JpaRepository<FaixaConsumo, Long> {

    /**
     * Faixas da categoria pertencentes a tabelas ativas, priorizando a tabela
     * vigente mais recente. O cálculo usa as faixas da primeira tabela retornada.
     */
    @Query("""
            select f from FaixaConsumo f
            join fetch f.tabela t
            where f.categoria = :categoria and t.ativo = true
            order by t.dataVigencia desc, t.id desc, f.inicio asc
            """)
    List<FaixaConsumo> findVigentesPorCategoria(@Param("categoria") Categoria categoria);
}
