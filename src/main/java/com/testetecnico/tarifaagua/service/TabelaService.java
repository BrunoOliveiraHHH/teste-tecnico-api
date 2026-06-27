package com.testetecnico.tarifaagua.service;

import com.testetecnico.tarifaagua.domain.Categoria;
import com.testetecnico.tarifaagua.domain.FaixaConsumo;
import com.testetecnico.tarifaagua.domain.TabelaTarifaria;
import com.testetecnico.tarifaagua.dto.CategoriaFaixasRequest;
import com.testetecnico.tarifaagua.dto.CriarTabelaRequest;
import com.testetecnico.tarifaagua.dto.FaixaRequest;
import com.testetecnico.tarifaagua.dto.TabelaResponse;
import com.testetecnico.tarifaagua.exception.RecursoNaoEncontradoException;
import com.testetecnico.tarifaagua.exception.RegraNegocioException;
import com.testetecnico.tarifaagua.repository.TabelaTarifariaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gerenciamento das tabelas tarifárias: criação (com validação de consistência
 * das faixas), listagem e exclusão lógica (soft delete).
 */
@Service
public class TabelaService {

    private final TabelaTarifariaRepository repository;

    public TabelaService(TabelaTarifariaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TabelaResponse criar(CriarTabelaRequest req) {
        validar(req);

        TabelaTarifaria tabela = new TabelaTarifaria();
        tabela.setNome(req.nome());
        tabela.setDataVigencia(req.dataVigencia() != null ? req.dataVigencia() : LocalDate.now());
        tabela.setAtivo(true);

        for (CategoriaFaixasRequest cat : req.categorias()) {
            for (FaixaRequest f : cat.faixas()) {
                FaixaConsumo faixa = new FaixaConsumo();
                faixa.setCategoria(cat.categoria());
                faixa.setInicio(f.inicio());
                faixa.setFim(f.fim());
                faixa.setValorUnitario(f.valorUnitario());
                tabela.adicionarFaixa(faixa);
            }
        }

        return toResponse(repository.save(tabela));
    }

    @Transactional(readOnly = true)
    public List<TabelaResponse> listar() {
        return repository.findByAtivoTrueOrderByDataVigenciaDescIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    /** Exclusão lógica: a tabela deixa de ser usada em cálculos, preservando o histórico. */
    @Transactional
    public void excluir(Long id) {
        TabelaTarifaria tabela = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Tabela tarifária não encontrada para o identificador " + id));
        tabela.setAtivo(false);
        repository.save(tabela);
    }

    // ----- validação de consistência das faixas -----

    private void validar(CriarTabelaRequest req) {
        Set<Categoria> vistas = EnumSet.noneOf(Categoria.class);
        for (CategoriaFaixasRequest cat : req.categorias()) {
            if (!vistas.add(cat.categoria())) {
                throw new RegraNegocioException("Categoria duplicada na tabela: " + cat.categoria());
            }
            validarFaixas(cat.categoria(), cat.faixas());
        }
    }

    private void validarFaixas(Categoria categoria, List<FaixaRequest> faixas) {
        List<FaixaRequest> ordenadas = faixas.stream()
                .sorted(Comparator.comparingInt(FaixaRequest::inicio))
                .toList();

        if (ordenadas.get(0).inicio() != 0) {
            throw new RegraNegocioException(
                    "A primeira faixa da categoria " + categoria + " deve iniciar em 0 m³.");
        }

        for (int i = 0; i < ordenadas.size(); i++) {
            FaixaRequest atual = ordenadas.get(i);

            if (atual.inicio() >= atual.fim()) {
                throw new RegraNegocioException("Faixa inválida na categoria " + categoria
                        + ": o início (" + atual.inicio() + ") deve ser menor que o fim (" + atual.fim() + ").");
            }

            if (i > 0) {
                FaixaRequest anterior = ordenadas.get(i - 1);
                if (atual.inicio() <= anterior.fim()) {
                    throw new RegraNegocioException("As faixas da categoria " + categoria
                            + " se sobrepõem entre [" + anterior.inicio() + "-" + anterior.fim()
                            + "] e [" + atual.inicio() + "-" + atual.fim() + "].");
                }
                if (atual.inicio() != anterior.fim() + 1) {
                    throw new RegraNegocioException("Cobertura incompleta na categoria " + categoria
                            + ": há uma lacuna entre " + anterior.fim() + " e " + atual.inicio() + ".");
                }
            }
        }
    }

    // ----- mapeamento -----

    private TabelaResponse toResponse(TabelaTarifaria tabela) {
        Map<Categoria, List<TabelaResponse.FaixaResponse>> porCategoria = tabela.getFaixas().stream()
                .sorted(Comparator.comparingInt(FaixaConsumo::getInicio))
                .collect(Collectors.groupingBy(
                        FaixaConsumo::getCategoria,
                        () -> new EnumMap<>(Categoria.class),
                        Collectors.mapping(f -> new TabelaResponse.FaixaResponse(
                                f.getId(), f.getInicio(), f.getFim(), f.getValorUnitario()), Collectors.toList())));

        List<TabelaResponse.CategoriaFaixasResponse> categorias = porCategoria.entrySet().stream()
                .map(e -> new TabelaResponse.CategoriaFaixasResponse(e.getKey().name(), e.getValue()))
                .toList();

        return new TabelaResponse(
                tabela.getId(), tabela.getNome(), tabela.getDataVigencia(), tabela.isAtivo(), categorias);
    }
}
