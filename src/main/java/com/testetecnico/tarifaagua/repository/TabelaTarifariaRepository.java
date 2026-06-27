package com.testetecnico.tarifaagua.repository;

import com.testetecnico.tarifaagua.domain.TabelaTarifaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TabelaTarifariaRepository extends JpaRepository<TabelaTarifaria, Long> {

    /** Tabelas ativas, da vigência mais recente para a mais antiga. */
    List<TabelaTarifaria> findByAtivoTrueOrderByDataVigenciaDescIdDesc();
}
